package io.middlepoint.tvsleep.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.MainActivityViewModel
import io.middlepoint.tvsleep.services.OverlayService
import io.middlepoint.tvsleep.services.WebServerService

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DebugScreen(
    navController: NavController,
    viewModel: MainActivityViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Waiting status") }
    viewModel.outputText.observeAsState().value?.let {
        status = it
    }

    Column(
        modifier =
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.33f),
    ) {
        Text(status)
    }

    Column(
        modifier =
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.33f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = {
                try {
                    requestOverlayPermission(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                    viewModel.sendCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
                }
            },
        ) {
            Text("Request Overlay Permission")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                context.startService(
                    (
                        Intent(
                            context,
                            OverlayService::class.java,
                        )
                    ),
                )
            },
        ) {
            Text("Show Overlay")
        }

        Spacer(modifier = Modifier.height(20.dp))

//        Button(
//            onClick = { viewModel.startTimer(30000) },
//        ) {
//            Text("Start Timer")
//        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                context.startService(
                    (
                        Intent(
                            context,
                            WebServerService::class.java,
                        )
                    ),
                )
            },
        ) {
            Text("Start Web Server")
        }

        Button(
            onClick = {
                context.stopService(
                    (
                        Intent(
                            context,
                            WebServerService::class.java,
                        )
                    ),
                )
            },
        ) {
            Text("Stop Web Server")
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
