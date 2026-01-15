package scot.raven.titanpad.shortcuts

import android.app.Activity
import android.os.Bundle
import scot.raven.titanpad.accessibility.AppAccessibilityService

class StandardCursorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppAccessibilityService.activateStandardCursor(this)
        finish()
    }
}