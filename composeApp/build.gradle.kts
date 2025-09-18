import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.node.js)
}

kotlin {
    jvmToolchain(20)
    androidTarget()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer =
                    (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                        static =
                            (static ?: mutableListOf()).apply {
                                // Serve sources to debug inside browser
                                add(project.rootDir.path)
                                add(project.projectDir.path)
                            }
                    }
            }
        }
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.compose.ui.util)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.constraintlayout.compose.multiplatform)

            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.ui)
            implementation(libs.androidx.ui.graphics)
            implementation(libs.androidx.ui.tooling.preview)
            implementation(libs.androidx.tv.foundation)
            implementation(libs.androidx.tv.material)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.androidx.activity.compose)

            implementation(libs.androidx.splash)

            implementation(libs.smoothmotion)
            implementation(libs.kermit)
            implementation(libs.androidx.preference.ktx)
            implementation(libs.androidx.lifecycle.process)

            // Ktor
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.cors)
            implementation(libs.ktor.server.default.headers)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.server.compression)
            implementation(libs.ktor.server.conditional.headers)
            implementation(libs.ktor.server.partial.content)
            implementation(libs.ktor.server.auto.head.response)

            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.network.tls.certificates)
        }

        wasmJsMain.dependencies {
        }
    }
}

android {
    namespace = "io.middlepoint.tvsleep"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.middlepoint.tvsleep"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_20
        targetCompatibility = JavaVersion.VERSION_20
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/*"
        }

        jniLibs {
            useLegacyPackaging = true
        }
    }
}
