package io.middlepoint.tvsleep.utils

import android.content.Context
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppUsageForegroundWatcher(
  private val context: Context,
  private val onPackageChanged: (String?) -> Unit
) {

  private var job: Job? = null

  fun start(periodMs: Long = 1000L) {
    if (job?.isActive == true) return
    job = CoroutineScope(Dispatchers.Default).launch {
      var lastPkg: String? = null
      while (isActive) {
        val pkg = ForegroundAppDetector.getLastForegroundPackage(context) // TODO: fallback to dumpsys?
        if (pkg != lastPkg) {
          lastPkg = pkg
          onPackageChanged(pkg)
        }
        delay(periodMs)
      }
    }
  }

  fun stop() {
    job?.cancel()
    job = null
  }
}
