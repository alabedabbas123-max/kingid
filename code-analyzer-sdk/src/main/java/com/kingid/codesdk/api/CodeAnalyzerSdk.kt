package com.kingid.codesdk.api

import android.content.Context
import androidx.annotation.MainThread
import com.kingid.codesdk.threading.AnalysisDispatcher

/**
 * نقطة الدخول الرئيسية لمكتبة KingID Code Analyzer SDK.
 *
 * مثال الاستخدام (واجهة fluent):
 * <pre>
 * CodeAnalyzerSdk.initialize(context)
 *     .withConfig(projectConfig)
 *     .createSession()
 *     .attachEditor(myEditor)
 *     .enableRealTime(true)
 *     .setDebounceMs(300)
 *     .start()
 * </pre>
 *
 * المكتبة مستقلة تماماً عن التطبيق المضيف (Standalone) وتُحزم كـ AAR.
 */
object CodeAnalyzerSdk {

    private var initialized = false

    /**
     * تهيئة المكتبة once. يجب استدعاؤها قبل أي عملية تحليل.
     * آمنة للاستدعاء المتكرر (idempotent).
     */
    @MainThread
    fun initialize(context: Context): Initializer {
        val appContext = context.applicationContext
        AnalysisDispatcher.init(appContext)
        initialized = true
        return Initializer(appContext)
    }

    /** إنشاء جلسة فحص مباشرة بإعدادات مخصصة. */
    @MainThread
    fun createSession(config: ProjectConfig): AnalyzerSession {
        requireInitialized()
        return AnalyzerSession(config)
    }

    /** إنشاء جلسة بإعدادات افتراضية. */
    @MainThread
    fun createSession(context: Context): AnalyzerSession {
        requireInitialized()
        return AnalyzerSession(ProjectConfig.default(context))
    }

    private fun requireInitialized() {
        if (!initialized) {
            throw IllegalStateException(
                "CodeAnalyzerSdk.initialize(context) يجب استدعاؤها قبل استخدام المكتبة"
            )
        }
    }

    /** منشئ عابر (Fluent Builder) لضبط الإعدادات قبل إنشاء الجلسة. */
    class Initializer internal constructor(private val appContext: Context) {
        private var config: ProjectConfig? = null

        fun withConfig(config: ProjectConfig): Initializer {
            this.config = config
            return this
        }

        @MainThread
        fun createSession(): AnalyzerSession {
            val c = config ?: ProjectConfig.default(appContext)
            return CodeAnalyzerSdk.createSession(c)
        }
    }
}
