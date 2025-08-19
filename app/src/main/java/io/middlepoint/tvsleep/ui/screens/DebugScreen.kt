package io.middlepoint.tvsleep.ui.screens

import android.content.Intent
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
import androidx.navigation.NavController
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.MainActivity
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
                    (context as? MainActivity)?.requestOverlayPermission(context)
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

        Button(
            onClick = { viewModel.startTimer(30000) },
        ) {
            Text("Start Timer")
        }

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
