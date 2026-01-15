package scot.raven.titanpad.shortcuts

import android.app.Activity
import android.os.Bundle
import scot.raven.titanpad.accessibility.AppAccessibilityService

class ToggleCursorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppAccessibilityService.toggleCursorScroll(this)
        finish()
    }
}