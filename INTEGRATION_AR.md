# KingID Code Analyzer SDK — دليل الدمج والاستدعاء في مشاريع أندرويد الخارجية

يشرح هذا المستند كيفية دمج مكتبة KingID في أي تطبيق أندرويد خارجي، تهيئة محركات الفحص، إلحاق المحرر، واستهلاك نتائج الفحص والإصلاحات السريعة.

---

## 1. إضافة المكتبة إلى المشروع

انظر ملف `BUILD_AR.md` للطريقتين (وحدة محلية أو ملف AAR). بعد الإضافة، تأكد من مزامنة Gradle.

---

## 2. تهيئة المكتبة (مرة واحدة)

في فئة `Application` أو في أول شاشة (`Activity`) عند بدء التطبيق:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // تهيئة واحدة وآمنة للتكرار
        CodeAnalyzerSdk.initialize(applicationContext)
    }
}
```

---

## 3. إنشاء إعدادات الجلسة (ProjectConfig)

```kotlin
val projectConfig = ProjectConfig(
    context = applicationContext,
    projectId = "my-project-001",
    sourceRoots = listOf(
        filesDir.absolutePath + "/src",
        externalFilesDir?.absolutePath
    ),
    javaEnabled = true,
    kotlinEnabled = true,
    minApiLevel = 21,
    targetApiLevel = 34,
    checkDeprecatedApis = true,
    deprecatedApiList = setOf("android.os.AsyncTask", "android.app.ProgressDialog"),
    debounceMs = 300,
    maxCacheEntries = 500,
    listener = object : AnalysisListener {
        override fun onAnalysisStarted() {
            // عرض مؤشر تحميل في واجهة المستخدم
        }
        override fun onAnalysisCompleted(result: AnalysisResult) {
            // إخفاء المؤشر، عرض ملخص النتائج
            Log.d("KingID", "أكتمل الفحص: ${result.errorsCount} خطأ، ${result.warningsCount} تحذير")
        }
        override fun onFixesReady(
            diagnostics: List<Diagnostic>,
            fixes: Map<Diagnostic, List<QuickFix>>
        ) {
            // عرض قائمة الإصلاحات السريعة للمستخدم
        }
        override fun onError(throwable: Throwable) {
            Log.e("KingID", "فشل الفحص", throwable)
        }
    }
)
```

---

## 4. إنشاء الجلسة وإلحاق المحرر

```kotlin
val analyzer = CodeAnalyzerSdk.createSession(projectConfig)

// إلحاق محرر الكود في تطبيقك (يجب أن ينفذ EditorAttachable)
analyzer.attachEditor(myCodeEditor)

// تفعيل الفحص اللحظي مع مهلة Debounce 300 ميلي ثانية
analyzer.enableRealTime(true).setDebounceMs(300).start()
```

### واجهة المحرر (EditorAttachable) — تنفيذexample

```kotlin
class MyCodeEditor(private val editText: EditText) : EditorAttachable {
    override val text: String get() = editText.text.toString()

    override fun setOnTextChangeListener(listener: (text: String, range: ChangeRange) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val t = s?.toString() ?: ""
                listener(t, ChangeRange(0, t.length, 0, t.length))
            }
        })
    }

    override fun applyFix(fix: QuickFix, callback: (Boolean) -> Unit) {
        // تطبيق التعديل الأول على موضع الخطأ في EditText
        val start = fix.location.startOffset.coerceAtLeast(0).coerceAtMost(editText.text.length)
        val end = fix.location.endOffset.coerceAtLeast(start).coerceAtMost(editText.text.length)
        val replacement = fix.edits.firstOrNull()?.replacement ?: ""
        editText.text.replace(start, end, replacement)
        callback(true)
    }
}
```

---

## 5. تشغيل الفحص حسب الطلب

```kotlin
// فحص مقتطف نصي (تزايدي، خلفي، غير متوقف)
val snippetResult = analyzer.analyzeSnippet(editText.text.toString(), null, "<editor>")

// فحص ملف واحد
val fileResult = analyzer.analyzeFile("/sdcard/project/MainActivity.java")

// فحص كامل المشروع (Full Project Analysis)
val projectResult = analyzer.analyzeProject()

// قراءة النتائج
projectResult.diagnostics.forEach { d ->
    println("${d.severity.label}: ${d.message} في ${d.location.file}:${d.location.startLine}")
}
```

---

## 6. استهلاك الإصلاحات السريعة (Quick Fixes)

```kotlin
val diagnostic = projectResult.diagnostics.firstOrNull { it.fixable } ?: return
val fixes = analyzer.getQuickFixes(diagnostic)

// عرض الإصلاحات في واجهة المستخدم (مثلاً نافذة منبثقة أو قائمة)
fixes.forEach { fix ->
    println("إصلاح: ${fix.title} — ${fix.description}")
}

// تطبيق إصلاح محدد
analyzer.applyFix(fixes.first()) {
    println("تم تطبيق الإصلاح بنجاح")
}
```

---

## 7. إدارة دورة الحياة

```kotlin
// عند إغلاق الشاشة أو Fragment
analyzer.stop()               // إيقاف المراقبة اللحظية
analyzer.detachEditor()       // فصل المحرر وإلغاء المهام
// أو لإفراغ الذاكرة بالكامل:
analyzer.clearCache()
```

---

## 8. مثال كامل (من التطبيق التجريبي `app/` في هذا المستودع)

راجع `app/src/main/java/com/kingid/demo/MainActivity.kt` الذي يُظهر:

1. تهيئة `CodeAnalyzerSdk.initialize(applicationContext)`.
2. إنشاء `ProjectConfig` مع `AnalysisListener`.
3. إنشاء جلسة وإلحاق `EditText` عبر `EditorAttachable`.
4. تفعيل `enableRealTime(true)`.
5. استدعاء `analyzeSnippet` و `analyzeProject` عند الضغط على أزرار.
6. عرض التشخيصات والإصلاحات السريعة في `TextView`.

---

## 9. ملاحظات الأداء

| الإعداد | التوصية |
|----------|----------|
| `debounceMs` | 250–500 ميلي ثانية للتحرير اللحظي؛ أعلى للفحص الخلفي |
| `maxCacheEntries` | 200–1000 حسب حجم المشروع وذاكرة الجهاز |
| `sourceRoots` | حدد فقط مسارات المصادر الفعلية لتفادي مسح ملفات غير ذات صلة |
| `checkDeprecatedApis` | فعّل فقط عند الحاجة لتقليل أعباء الفحص |

---

## 10. استكشاف الأخطاء

- **مكتبة غير مهيأة**: تأكد من استدعاء `CodeAnalyzerSdk.initialize(context)` قبل `createSession()`.
- **عدم ظهور تشخيصات**: تأكد من أن امتدادات الملفات في `sourceRoots` تتوافق مع `supportedExtensions` للمحللين المسجلين (افتراضياً `.java` و `.kt`).
- **تسرب ذاكرة**: استدعي `stop()` / `detachEditor()` عند تدمير واجهة المستخدم lest تُحتفظ بالكوروتينات.
- **حجم AAR كبير**: الـ AAR الأساسي يحتوي فقط على Kotlin stdlib و Coroutines و AndroidX Core؛ أي مكتبات AST خارجية تُضاف من قبل التطبيق المضيف فقط.
