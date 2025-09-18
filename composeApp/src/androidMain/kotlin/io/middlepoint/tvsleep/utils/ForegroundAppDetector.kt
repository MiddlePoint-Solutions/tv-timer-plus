package io.middlepoint.tvsleep.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

object ForegroundAppDetector {

  /**
   * Returns the package name of the most recent foreground activity/app,
   * or null if none could be determined.
   *
   * Uses UsageEvents:
   *  - On API 29+: prefers ACTIVITY_RESUMED/ACTIVITY_PAUSED
   *  - Otherwise: falls back to MOVE_TO_FOREGROUND/MOVE_TO_BACKGROUND
   */
  fun getLastForegroundPackage(context: Context, lookbackMs: Long = 10_000L): String? {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val end = System.currentTimeMillis()
    val start = end - lookbackMs

    val events = usm.queryEvents(start, end)
    val event = UsageEvents.Event()
    var lastForegroundPkg: String? = null
    var lastFgTimestamp = 0L

    while (events.hasNextEvent()) {
      events.getNextEvent(event)

      val isResume =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
          event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }

      if (isResume && event.timeStamp >= lastFgTimestamp) {
        lastForegroundPkg = event.packageName
        lastFgTimestamp = event.timeStamp
      }
    }
    return lastForegroundPkg
  }
}
