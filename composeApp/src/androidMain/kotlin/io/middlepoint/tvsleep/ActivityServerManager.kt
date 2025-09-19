package io.middlepoint.tvsleep

import android.content.Context
import co.touchlab.kermit.Logger
import io.ktor.server.engine.ApplicationEngine
import io.middlepoint.tvsleep.services.createKtorWebServer
import io.middlepoint.tvsleep.utils.ADBOld // Assuming ADBOld is correctly located and set up
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ActivityServerManager private constructor(
    context: Context,
) {
    private val logger = Logger.withTag("ActivityServerManager")
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverJob: Job? = null
    private var serverInstance: ApplicationEngine? = createKtorWebServer(context)

    // Initialize ADBOld instance lazily using the application context
    private val adb by lazy { ADBOld.getInstance(context) }

    private val showStatusLambda: (String) -> Unit = { message ->
        logger.i { "Server Status: $message" }
    }

    private val sendShellCommandLambda: (String) -> Unit = { command ->
        logger.i { "Attempting to send shell command: $command" }
        try {
            // TODO: Verify that adb.sendToShellProcess(command) is the correct, non-blocking,
            // and safe way to execute shell commands in this context.
            // Consider implications of running shell commands from a potentially long-lived server.
            // adb.sendToShellProcess(command)
            logger.w { "Actual execution via ADBOld.sendToShellProcess is commented out. Review and test before enabling." }
        } catch (e: Exception) {
            logger.e("Error sending shell command", throwable = e)
        }
    }

    fun startServer() {
        if (serverJob?.isActive == true) {
            logger.i("Server is already running.")
            return
        }
        logger.i("Initializing and starting server...")
        serverJob =
            serverScope.launch {
                try {
                    serverInstance?.start(wait = true)
                } catch (e: Exception) {
                    logger.e("Error during server execution", throwable = e)
                }
            }
        logger.i("Server start process initiated.")
    }

    fun stopServer() {
        logger.i("Attempting to stop server...")
        serverJob?.cancel()
        serverInstance.let {
            try {
                // Ktor's stop is suspending, but ApplicationEngine.stop isn't.
                // Graceful shutdown with timeout.
                it?.stop(100L, 500L) // Grace period and timeout in milliseconds
                logger.i("Server explicit stop called.")
            } catch (e: Exception) {
                logger.e("Exception while stopping server instance", throwable = e)
            }
        }
        serverInstance = null // Clear the instance to allow re-creation if startServer is called again
        serverJob = null
        logger.i("Server stop process completed.")
    }

    fun cleanup() {
        logger.i("Cleaning up ActivityServerManager resources.")
        stopServer()
        serverScope.cancel() // Cancel the scope to clean up coroutines
    }

    companion object {
        @Volatile
        private var instance: ActivityServerManager? = null

        fun getInstance(context: Context): ActivityServerManager =
            instance ?: synchronized(this) {
                instance ?: ActivityServerManager(context.applicationContext).also { instance = it }
            }
    }
}
