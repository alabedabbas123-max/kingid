package com.kingid.codesdk.threading

import android.content.Context

/**
 * مؤشر Dispatcher يتولى تشغيل عمليات الفحص على الخيوط الخلفية.
 * يُعتمد على Dispatchers.Default من Kotlin Coroutines للمهام التي تستهلك معالجاً (CPU-bound).
 */
object AnalysisDispatcher {

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
    }
}
