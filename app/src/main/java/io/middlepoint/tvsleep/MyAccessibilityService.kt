package io.middlepoint.tvsleep

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for this purpose
    }

    override fun onInterrupt() {
        // Not used
    }

    // Optional: public static methods to trigger global actions
    companion object {
        var instance: MyAccessibilityService? = null
    }

    override fun onServiceConnected() {
        Log.d("MyAccessibilityService", "Service connected")
        super.onServiceConnected()
        instance = this
    }
}
