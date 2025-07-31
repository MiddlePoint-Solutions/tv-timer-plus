package io.middlepoint.tvsleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalTvMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    CoroutineScope(Dispatchers.IO).launch {
      embeddedServer(
        factory = Netty,
        configure = { envConfig() }) {
        routing {
          get("/") {
            call.respondText("Hello world")
          }
        }
      }.start(wait = true)
    }

    setContent {
      TVsleepTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          shape = RectangleShape
        ) {
          Greeting("Android")
        }
      }
    }
  }

  private fun ApplicationEngine.Configuration.envConfig() {

    val keyStoreFile = File(filesDir, "keystore.jks")
    val keyStore = buildKeyStore {
      certificate("sampleAlias") {
        password = "foobar"
        domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
      }
    }
    keyStore.saveToFile(keyStoreFile, "123456")

    connector {
      port = 3112
    }

    sslConnector(
      keyStore = keyStore,
      keyAlias = "sampleAlias",
      keyStorePassword = { "123456".toCharArray() },
      privateKeyPassword = { "foobar".toCharArray() }) {
      port = 3113
      keyStorePath = keyStoreFile
    }
  }

}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name!",
    modifier = modifier
  )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  TVsleepTheme {
    Greeting("Android")
  }
}