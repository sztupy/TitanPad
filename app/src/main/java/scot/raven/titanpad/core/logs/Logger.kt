package scot.raven.titanpad.core.logs

import android.util.Log
import rikka.shizuku.shared.BuildConfig

object Logger {
    private const val TAG = "TitanPadApp"

    enum class Level {
        VERBOSE,
        DEBUG,
        INFO,
        WARNING,
        ERROR,
    }

    private val minLogLevel = if (BuildConfig.DEBUG) Level.VERBOSE else Level.WARNING

    fun v(
        message: String,
        tag: String? = null,
    ) {
        if (minLogLevel.ordinal <= Level.VERBOSE.ordinal) {
            Log.v(tag ?: TAG, message)
        }
        LogManager.addLog(Level.VERBOSE, message, tag)
    }

    fun d(
        message: String,
        tag: String? = null,
    ) {
        if (minLogLevel.ordinal <= Level.DEBUG.ordinal) {
            Log.d(tag ?: TAG, message)
        }
        LogManager.addLog(Level.DEBUG, message, tag)
    }

    fun i(
        message: String,
        tag: String? = null,
    ) {
        if (minLogLevel.ordinal <= Level.INFO.ordinal) {
            Log.i(tag ?: TAG, message)
        }
        LogManager.addLog(Level.INFO, message, tag)
    }

    fun w(
        message: String,
        throwable: Throwable? = null,
        tag: String? = null,
    ) {
        if (throwable != null) {
            if (minLogLevel.ordinal <= Level.WARNING.ordinal) {
                Log.w(tag ?: TAG, message, throwable)
            }
            LogManager.addLog(Level.WARNING, "$message: ${throwable.message}", tag)
        } else {
            if (minLogLevel.ordinal <= Level.WARNING.ordinal) {
                Log.w(tag ?: TAG, message)
            }
            LogManager.addLog(Level.WARNING, message, tag)
        }
    }

    fun e(
        message: String,
        throwable: Throwable? = null,
        tag: String? = null,
    ) {
        if (throwable != null) {
            if (minLogLevel.ordinal <= Level.ERROR.ordinal) {
                Log.e(tag ?: TAG, message, throwable)
            }
            LogManager.addLog(Level.ERROR, "$message: ${throwable.message}", tag)
        } else {
            if (minLogLevel.ordinal <= Level.ERROR.ordinal) {
                Log.e(tag ?: TAG, message)
            }
            LogManager.addLog(Level.ERROR, message, tag)
        }
    }
}