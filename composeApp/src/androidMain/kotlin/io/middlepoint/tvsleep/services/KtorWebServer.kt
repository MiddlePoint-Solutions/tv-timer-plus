package io.middlepoint.tvsleep.services

import android.content.Context
import android.content.res.AssetManager
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.matchContentType
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val ASSETS_BASE = "wasmapp"

fun createKtorWebServer(context: Context) =
    embeddedServer(
        factory = CIO,
        port = 3112,
    ) {
        install(AutoHeadResponse)
        install(PartialContent) // Range requests
        install(ConditionalHeaders) // ETag/Last-Modified
        install(DefaultHeaders) {
            // Needed for SAB/threads in many Wasm toolchains
            header("Cross-Origin-Opener-Policy", "same-origin")
            header("Cross-Origin-Embedder-Policy", "require-corp")
        }
        // Allow opening from other devices on the LAN (tighten if you like)
        install(CORS) {
            anyHost()
            allowNonSimpleContentTypes = true
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.AcceptEncoding)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Head)
            allowMethod(HttpMethod.Options)
        }

        // Don’t compress .wasm on the fly; precompress instead. Keep text types compressed.
        install(Compression) {
            gzip {
                priority = 1.0
                matchContentType(
                    ContentType.Text.Any,
                    ContentType.Application.Json,
                    ContentType.Application.JavaScript,
                )
            }
        }

        routing {
//            staticResources("/", "assets/$ASSETS_BASE")
            serveWasmFromAssets(context)
        }
    }

fun Route.serveWasmFromAssets(context: Context) {
    get("/") {
        respondAsset(
            call,
            context.assets,
            "$ASSETS_BASE/index.html",
            contentType = ContentType.Text.Html,
            cache = "no-cache",
        )
    }

    get("/{path...}") {
        val rel = call.parameters.getAll("path")?.joinToString("/") ?: ""
        val am = context.assets

        // SPA fallback: URLs without a dot => index.html
        val isAsset = '.' in rel
        val assetPath =
            when {
                isAsset && assetExists(am, "$ASSETS_BASE/$rel") -> "$ASSETS_BASE/$rel"
                !isAsset -> "$ASSETS_BASE/index.html"
                else -> "$ASSETS_BASE/$rel"
            }

        val (ct, cache) =
            when {
                assetPath.endsWith(".html", true) -> ContentType.Text.Html to "no-cache"
                assetPath.endsWith(".js", true) -> ContentType.Application.JavaScript to "public, max-age=31536000, immutable"
                assetPath.endsWith(".mjs", true) -> ContentType.Application.JavaScript to "public, max-age=31536000, immutable"
                assetPath.endsWith(".css", true) -> ContentType.Text.CSS to "public, max-age=31536000, immutable"
                assetPath.endsWith(".json", true) -> ContentType.Application.Json to "public, max-age=31536000, immutable"
                assetPath.endsWith(".wasm", true) -> ContentType.parse("application/wasm") to "public, max-age=31536000, immutable"
                else -> ContentType.defaultForFilePath(assetPath.substringAfterLast('/')) to "public, max-age=86400"
            }

        // Debug: see what Chrome actually offered
        android.util.Log.d("KtorLAN", "GET /$rel  Accept-Encoding=${call.request.headers[HttpHeaders.AcceptEncoding]}")

        // Prefer precompressed .br/.gz for non-HTML
        if (!assetPath.endsWith(".html", true)) {
            val encs =
                call.request.headers[HttpHeaders.AcceptEncoding]
                    ?.split(',')
                    ?.map { it.trim().substringBefore(';').lowercase() } ?: emptyList()

            val brPath = "$assetPath.br"
            if ("br" in encs && assetExists(am, brPath)) {
                respondAssetCompressed(call, am, brPath, ct, cache, contentEncoding = "br")
                return@get
            }

            val gzPath = "$assetPath.gz"
            if ("gzip" in encs && assetExists(am, gzPath)) {
                respondAssetCompressed(call, am, gzPath, ct, cache, contentEncoding = "gzip")
                return@get
            }
        }

        // Fallback to the original
        respondAsset(call, am, assetPath, ct, cache)
    }
}

private fun assetExists(
    am: android.content.res.AssetManager,
    path: String,
): Boolean =
    try {
        am.open(path).use { }
        true
    } catch (_: java.io.IOException) {
        false
    }

private suspend fun respondAsset(
    call: ApplicationCall,
    am: AssetManager,
    path: String,
    contentType: ContentType,
    cache: String,
) {
    try {
        call.response.headers.append(HttpHeaders.CacheControl, cache, false)
        // Tell caches that representation varies by Accept-Encoding
        call.response.headers.append(HttpHeaders.Vary, HttpHeaders.AcceptEncoding, false)

        call.respondOutputStream(contentType = contentType) {
            am.open(path).use { it.copyTo(this) }
        }
    } catch (_: java.io.IOException) {
        call.respond(HttpStatusCode.NotFound)
    }
}

private suspend fun respondAssetCompressed(
    call: ApplicationCall,
    am: AssetManager,
    path: String,
    contentType: ContentType,
    cache: String,
    contentEncoding: String,
) {
    try {
        call.response.headers.append(HttpHeaders.ContentEncoding, contentEncoding, false)
        call.response.headers.append(HttpHeaders.CacheControl, cache, false)
        call.response.headers.append(HttpHeaders.Vary, HttpHeaders.AcceptEncoding, false)
        call.respondOutputStream(contentType = contentType) {
            am.open(path).use { it.copyTo(this) }
        }
    } catch (_: java.io.IOException) {
        call.respond(HttpStatusCode.NotFound)
    }
}
