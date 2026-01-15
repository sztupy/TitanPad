package scot.raven.titanpad.shortcuts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import scot.raven.titanpad.R

class ToggleCursorCreateShortcutActivity : Activity() {
    @Suppress("Deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shortcutIntent = Intent(Intent.ACTION_VIEW)
        shortcutIntent.setClassName(packageName, "scot.raven.titanpad.shortcuts.ToggleCursorActivity")

        val intent = Intent()
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Toggle Cursor Scroll")
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher))

        setResult(RESULT_OK, intent)
        finish()
    }
}