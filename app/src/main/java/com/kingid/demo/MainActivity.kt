package com.kingid.demo

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kingid.codesdk.api.*
import com.kingid.codesdk.core.*

class MainActivity : AppCompatActivity(), EditorAttachable {

    private lateinit var editor: EditText
    private lateinit var tvStatus: TextView
    private lateinit var tvResults: TextView
    private lateinit var session: AnalyzerSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        tvStatus = findViewById(R.id.tvStatus)
        tvResults = findViewById(R.id.tvResults)

        // 1. تهيئة المكتبة مرة واحدة عند启动 التطبيق
        CodeAnalyzerSdk.initialize(applicationContext)

        // 2. إنشاء جلسة تحليل بإعدادات المشروع
        val config = ProjectConfig(
            context = applicationContext,
            projectId = "demo-project",
            minApiLevel = 21,
            targetApiLevel = 34,
            checkDeprecatedApis = true,
            debounceMs = 400,
            listener = object : AnalysisListener {
                override fun onAnalysisStarted() = runOnUiThread { tvStatus.text = "جاري التحليل..." }
                override fun onAnalysisCompleted(result: AnalysisResult) = runOnUiThread {
                    tvStatus.text = "انتهى التحليل: ${result.errorsCount} أخطاء، ${result.warningsCount} تحذير (${result.durationMs} ميلي ثانية)"
                }
                override fun onFixesReady(diagnostics: List<Diagnostic>, fixes: Map<Diagnostic, List<QuickFix>>) = runOnUiThread {
                    renderFixes(diagnostics, fixes)
                }
                override fun onError(throwable: Throwable) = runOnUiThread {
                    tvStatus.text = "خطأ: ${throwable.message}"
                }
            }
        )
        session = CodeAnalyzerSdk.createSession(config)

        // 3. إلحاق المحرر بالجلسة لتفعيل المراقبة اللحظية (Real-time)
        session.attachEditor(this).enableRealTime(true).setDebounceMs(400).start()

        findViewById<Button>(R.id.btnAnalyze).setOnClickListener {
            // تحليل مقتطف النص الحالي فوراً (خلفي، غير متوقف)
            val result = session.analyzeSnippet(editor.text.toString(), null, "<editor>")
            tvStatus.text = "اكتمل الفحص اللحظي: ${result.errorsCount} أخطاء، ${result.warningsCount} تحذير"
            renderResult(result)
        }

        findViewById<Button>(R.id.btnFull).setOnClickListener {
            // فحص كامل المشروع (مثال على مسارات المصادر)
            val result = session.analyzeProject()
            tvStatus.text = "انتهى الفحص الكامل للمشروع"
            renderResult(result)
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            tvResults.text = ""
            tvStatus.text = resources.getString(R.string.status_ready)
        }
    }

    /* ----------- تنفيذ EditorAttachable ----------- */
    override val text: String get() = editor.text.toString()

    override fun setOnTextChangeListener(listener: (text: String, range: ChangeRange) -> Unit) {
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val t = s?.toString() ?: ""
                listener(t, ChangeRange(0, t.length, 0, t.length))
            }
        })
    }

    override fun applyFix(fix: QuickFix, callback: (Boolean) -> Unit) {
        // مثال تطبيق تعديل بسيط: إدراج نص الإصلاح في موضع الخطأ
        val start = fix.location.startOffset.coerceAtLeast(0).coerceAtMost(editor.text.length)
        val end = fix.location.endOffset.coerceAtLeast(start).coerceAtMost(editor.text.length)
        editor.text.replace(start, end, fix.edits.firstOrNull()?.replacement ?: "")
        callback(true)
    }

    /* ----------- عرض النتائج ----------- */
    private fun renderResult(result: AnalysisResult) {
        val sb = StringBuilder()
        sb.append("الملف: ").append(result.file).append("\n")
        sb.append("المخزّن مؤقتاً: ").append(result.cached).append(" | المدة: ").append(result.durationMs).append(" مللي ثانية\n\n")
        result.diagnostics.forEach { d ->
            sb.append("[${d.severity.label}] ").append(d.code).append(": ").append(d.message).append("\n")
        }
        tvResults.text = sb.toString()
    }

    private fun renderFixes(diagnostics: List<Diagnostic>, fixes: Map<Diagnostic, List<QuickFix>>) {
        val sb = StringBuilder("الإصلاحات السريعة المقترحة:\n\n")
        diagnostics.forEach { d ->
            val list = fixes[d] ?: emptyList()
            sb.append("- ").append(d.message).append("\n")
            list.forEach { f ->
                sb.append("    • ").append(f.title).append(" (id: ${f.id})\n")
            }
        }
        tvResults.append("\n" + sb.toString())
    }
}
