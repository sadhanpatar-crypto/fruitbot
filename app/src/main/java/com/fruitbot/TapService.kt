package com.fruitbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/** Screen par tap karne ke liye Accessibility Service. */
class TapService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: TapService? = null
    }

    private val main = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun tap(x: Float, y: Float) {
        main.post {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 30))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }
}
