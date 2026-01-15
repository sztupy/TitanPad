package scot.raven.titanpad.shortcuts

import android.app.Activity
import android.os.Bundle
import scot.raven.titanpad.accessibility.AppAccessibilityService

class ResetGridActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppAccessibilityService.resetGrid(this)
        finish()
    }
}