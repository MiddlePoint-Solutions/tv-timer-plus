package io.middlepoint.tvsleep

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_FOCUS
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_SELECTION
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import co.touchlab.kermit.Logger
import dadb.Dadb
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private lateinit var serverJob: Job
  private val logger = Logger.withTag("MainActivity")

  private lateinit var devicePolicyManager: DevicePolicyManager
  private lateinit var deviceAdminComponent: ComponentName

  // ActivityResultLauncher to handle device admin activation
  private val adminActivationLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == RESULT_OK) {
      Toast.makeText(this, "Device Admin activated", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(this, "Device Admin activation cancelled", Toast.LENGTH_SHORT).show()
    }
  }

  @OptIn(ExperimentalTvMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
    deviceAdminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

    setContent {

      val viewModel by viewModels<MainActivityViewModel>()
      val coroutineScope = rememberCoroutineScope()
      var status by remember { mutableStateOf("Waiting status") }

      val serverJob = remember {
        coroutineScope.launch(Dispatchers.IO) {
          createWebServer(
            showStatus = { status = it },
            sendShellCommand = {
              lifecycleScope
                .launch(Dispatchers.IO) {
                  viewModel.adb.sendToShellProcess(it)
                }
            }
          )
        }
      }

      LaunchedEffect(Unit) {
        viewModel.outputText.observeForever {
          status = it
        }
      }

      LaunchedEffect(Unit) {
        viewModel.startADBServer {
          logger.d { "ADB server started: $it" }
        }
      }

      TVsleepTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          shape = RectangleShape
        ) {
          Column {
            Text(status)

            Spacer(modifier = Modifier.height(25.dp))

//            Button(onClick = {
//              if (!devicePolicyManager.isAdminActive(deviceAdminComponent)) {
//                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
//                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent)
//                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Explanation")
//                adminActivationLauncher.launch(intent)
//              } else {
//                Toast.makeText(this@MainActivity, "Device Admin already active", Toast.LENGTH_SHORT)
//                  .show()
//              }
//            }) {
//              Text("Enable Admin")
//            }
          }
        }
      }
    }
  }

  private fun CoroutineScope.createWebServer(
    showStatus: (String) -> Unit,
    sendShellCommand: (String) -> Unit,
  ) =
    embeddedServer(
      factory = CIO,
      port = 3112,
    ) {
      routing {
        get("/") {
          call.respondText("Hello world")
        }

        get("/clearFocus") {
          call.respondText("ACTION_CLEAR_FOCUS")
          MyAccessibilityService.instance?.performGlobalAction(ACTION_CLEAR_FOCUS)
        }

        get("/clearSelection") { // WORKING
          call.respondText("ACTION_CLEAR_SELECTION")
          // This is one way of closing the screen / after checking the available actions
          MyAccessibilityService.instance?.performGlobalAction(ACTION_CLEAR_SELECTION)
        }

        get("/check") {
          sendShellCommand("whoami")
          call.respondText("checking...")
        }

        get("/discover") {
          logger.d { "discover" }
          try {
            call.respondText {
              Dadb.create("localhost", 5555).use {
                logger.d { "created" }
                val response = it.shell("ls")
                showStatus(response.allOutput)
                response.allOutput
              }
            }
          } catch (e: Throwable) {
            logger.e("discover error", e)
          }
        }

        get("/disable/{packageId}") {
          // TODO: suspend app on API 28+ (Android 9+)
          // Disable app
          val packageId = call.parameters["packageId"]!!
          sendShellCommand("pm disable-user --user 0 $packageId")
          call.respondText("Disabled $packageId")
        }

        get("/enable/{packageId}") {
          val packageId = call.parameters["packageId"]!!
          sendShellCommand("pm enable --user 0 $packageId")
          call.respondText("Enabled $packageId")
        }

        get("/kill/{packageId}") {
          val packageId = call.parameters["packageId"]!!
          sendShellCommand("am force-stop $packageId")
          call.respondText("Killed $packageId")
        }

      }

      /*
        TODO: \
          Secure the application (when controls are active):
          disable system settings: com.android.tv.settings & com.google.android.packageinstaller
            This way we block the user from uninstalling the app.
       */
    }.start(wait = true)

  private fun launchAccessibilityService() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    startActivity(intent)
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