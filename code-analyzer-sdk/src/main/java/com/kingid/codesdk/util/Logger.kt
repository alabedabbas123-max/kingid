package com.kingid.codesdk.util

import android.util.Log

/**
 * مسجّل بسيط للمكتبة مع إمكانية إيقاف السجلات في الإصدارات finale عبر BuildConfig.
 */
object Logger {
    private const val TAG = "KingIDCodeSdk"
    var isDebugEnabled = true

    fun d(message: String, throwable: Throwable? = null) {
        if (isDebugEnabled) Log.d(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }
}
