package com.kingid.codesdk.api

import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.kingid.codesdk.analysis.Debouncer
import com.kingid.codesdk.analysis.PendingChange
import com.kingid.codesdk.cache.AnalysisCache
import com.kingid.codesdk.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/**
 * جلسة تحليل مستقلة. تمثل وحدة الفحص والإصلاح لمشروع أو ملف واحد.
 * جميع الطرق العامة تُستدعى على خيط الواجهة الرئيسية (Main Thread)؛
 * العمليات التي تستهلك المعالج تُنفذ في خلفية عبر Dispatchers.Default.
 */
class AnalyzerSession internal constructor(private val config: ProjectConfig) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engine = AnalysisEngine(config)
    private val cache: AnalysisCache = AnalysisCache.create(config.context.cacheDir, config.maxCacheEntries)
    private val fixEngine = FixEngine()
    private val listeners = mutableListOf<AnalysisListener>()

    private var attachedEditor: EditorAttachable? = null
    private var isRealTime = false
    private var status: AnalysisStatus = AnalysisStatus.IDLE

    /** إضافة مستمع لنتائج الفحص والإصلاحات. */
    fun addListener(listener: AnalysisListener): AnalyzerSession {
        listeners += listener
        return this
    }

    /** إزالة مستمع. */
    fun removeListener(listener: AnalysisListener): AnalyzerSession {
        listeners -= listener
        return this
    }

    /** إلحاق محرر المضيف بالجلسة لتفعيل المراقبة اللحظية للتغييرات. */
    @MainThread
    fun attachEditor(editor: EditorAttachable): AnalyzerSession {
        attachedEditor = editor
        editor.setOnTextChangeListener { text, range ->
            scheduleIncremental(text, range)
        }
        return this
    }

    /** فصل المحرر وإيقاف كل العمليات الجارية. */
    @MainThread
    fun detachEditor(): AnalyzerSession {
        stop()
        attachedEditor = null
        return this
    }

    /** تفعيل/تعطيل الفحص اللحظي عند تغيير النص. */
    @MainThread
    fun enableRealTime(enabled: Boolean): AnalyzerSession {
        isRealTime = enabled
        return this
    }

    /** تعيين مدة الت veio (Debounce) بالميلي ثانية بين التغيير وبدء الفحص. */
    @MainThread
    fun setDebounceMs(ms: Long): AnalyzerSession {
        engine.debouncer.delayMs = ms
        return this
    }

    /** إشارة بدء المراقبة اللحظية (يتم استدعاؤها بعد attachEditor). */
    @MainThread
    fun start() {
        // يُStarted الفحص تلقائياً عبر Debouncer عند كل تغيير في النص
    }

    /** إيقاف جميع العمليات الجارية لهذه الجلسة. */
    @MainThread
    fun stop() {
        engine.debouncer.cancel()
        scope.cancel()
        status = AnalysisStatus.IDLE
    }

    /** تحليل ملف واحد فوراً (يُنفذ في خيط خلفي). */
    @WorkerThread
    fun analyzeFile(filePath: String): AnalysisResult {
        status = AnalysisStatus.RUNNING
        return engine.analyzeFile(filePath, cache)
    }

    /** تحليل كامل المشروع (Full Project Analysis). */
    @WorkerThread
    fun analyzeProject(): AnalysisResult {
        status = AnalysisStatus.RUNNING
        return engine.analyzeProject(config.sourceRoots, cache, config.projectId)
    }

    /** تحليل مقتطف نصي (snippet) كجزء من الفحص التزايدي. */
    @WorkerThread
    fun analyzeSnippet(text: String, range: ChangeRange?, fileKey: String = "<editor>"): AnalysisResult {
        status = AnalysisStatus.RUNNING
        return engine.analyzeSnippet(text, range, fileKey, cache)
    }

    /** جلب الإصلاحات السريعة المقترحة لخطأ محدد. */
    @MainThread
    fun getQuickFixes(diagnostic: Diagnostic): List<QuickFix> {
        return fixEngine.proposeFixes(diagnostic)
    }

    /** تطبيق إصلاح سريع عبر المحرر الملحق. */
    @MainThread
    fun applyFix(fix: QuickFix, onApplied: () -> Unit) {
        attachedEditor?.applyFix(fix) { success ->
            if (success) mainHandler.post { onApplied.invoke() }
        }
    }

    /** مسح ذاكرة التخزين المؤقت للنتائج. */
    fun clearCache() {
        cache.clear()
    }

    /** حالة الجلسة الحالية. */
    val currentStatus: AnalysisStatus
        get() = status

    /* ------------------------------------------------------------------ */
    /* الفحص التزايدي عند تغيير النص (مُطلق من Debouncer)               */
    /* ------------------------------------------------------------------ */

    private fun scheduleIncremental(text: String, range: ChangeRange) {
        if (!isRealTime) return
        status = AnalysisStatus.RUNNING
        mainHandler.post { listeners.forEach { it.onAnalysisStarted() } }

        engine.debouncer.submit(text, range, "<editor>") { change ->
            scope.launch {
                val result = engine.analyzeSnippet(
                    change.text, change.range, change.fileKey, cache
                )
                mainHandler.post {
                    status = AnalysisStatus.COMPLETED
                    listeners.forEach { it.onAnalysisCompleted(result) }
                    val fixes = result.diagnostics.associateWith { d -> fixEngine.proposeFixes(d) }
                    listeners.forEach { it.onFixesReady(result.diagnostics, fixes) }
                }
            }
        }
    }
}
