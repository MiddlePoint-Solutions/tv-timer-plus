package io.middlepoint.tvsleep.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import co.touchlab.kermit.Logger
import dadb.Dadb
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.middlepoint.tvsleep.MyAccessibilityService
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.utils.ADB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WebServerService : Service() {
    private val logger = Logger.withTag("WebServerService")
    private val adb by lazy { ADB.getInstance(applicationContext) }
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var serverJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        logger.i { "WebServerService started" }

        serverJob =
            serviceScope.launch {
                createWebServer(
                    showStatus = { adb.debug(it) },
                    sendShellCommand = { adb.sendToShellProcess(it) },
                ).start(wait = true)
            }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.i { "WebServerService is being destroyed" }
        serverJob?.cancel()
        serviceJob.cancel()
    }

    private fun createNotification(): Notification {
        val channelId = "WebServerServiceChannel"
        val channelName = "Web Server Service"
        val chan =
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)

        return NotificationCompat
            .Builder(this, channelId)
            .setContentTitle("TV Sleep Server")
            .setContentText("Web server is running.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun createWebServer(
        showStatus: (String) -> Unit,
        sendShellCommand: (String) -> Unit,
    ) = embeddedServer(
        factory = CIO,
        port = 3112,
    ) {
        routing {
            get("/") {
                call.respondText("Hello world")
            }

            get("/clearFocus") {
                call.respondText("ACTION_CLEAR_FOCUS")
                MyAccessibilityService.Companion.instance?.performGlobalAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
            }

            get("/clearSelection") {
                // WORKING
                call.respondText("ACTION_CLEAR_SELECTION")
                MyAccessibilityService.Companion.instance?.performGlobalAction(AccessibilityNodeInfo.ACTION_CLEAR_SELECTION)
            }

            get("/check") {
                sendShellCommand("echo \"Hello there!\"")
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
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}
