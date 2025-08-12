package io.middlepoint.tvsleep

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import co.touchlab.kermit.Logger
import io.middlepoint.tvsleep.services.OverlayService
import io.middlepoint.tvsleep.services.WebServerService
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme

class MainActivity : ComponentActivity() {
    private val logger = Logger.withTag("MainActivity")

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var deviceAdminComponent: ComponentName

    // ActivityResultLauncher to handle device admin activation
    private val adminActivationLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
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
            var status by remember { mutableStateOf("Waiting status") }

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
                    shape = RectangleShape,
                ) {
                    Row {
                        Column(
                            modifier = Modifier.fillMaxWidth(0.4f),
                        ) {
                            Text(status)
                        }

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.6f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        requestOverlayPermission(this@MainActivity)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        viewModel.sendCommand("appops set $packageName SYSTEM_ALERT_WINDOW allow")
                                    }
                                },
                            ) {
                                Text("Request Overlay Permission")
                            }

                            Button(
                                onClick = { startService((Intent(this@MainActivity, OverlayService::class.java))) },
                            ) {
                                Text("Show Overlay")
                            }

                            Spacer(modifier = Modifier.height(25.dp))

                            Button(
                                onClick = { startService((Intent(this@MainActivity, WebServerService::class.java))) },
                            ) {
                                Text("Start Web Server")
                            }

                            Button(
                                onClick = { stopService((Intent(this@MainActivity, WebServerService::class.java))) },
                            ) {
                                Text("Stop Web Server")
                            }
                        }
                    }
                }
            }
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun launchAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    TVsleepTheme {
        Greeting("Android")
    }
}
