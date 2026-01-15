package scot.raven.titanpad.core.util

import android.os.Build

object VersionUtil {
    fun belowVersion(version: Int): Boolean {
        return Build.VERSION.SDK_INT < version
    }
}