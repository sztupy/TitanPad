package scot.raven.titanpad.shortcuts

import android.app.Activity
import android.os.Bundle
import scot.raven.titanpad.accessibility.AppAccessibilityService
import scot.raven.titanpad.settings.ui.SettingsActivity.Companion.CONFIG_ID_EXTRA

class StandardCursorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var configId = intent.getStringExtra(CONFIG_ID_EXTRA)
        if (configId==null)
            configId = "default"
        AppAccessibilityService.activateStandardCursor(this, configId)
        finish()
    }
}