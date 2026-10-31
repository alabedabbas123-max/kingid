# KingID Code Analyzer SDK — التصميم الهندسي التفصيلي

مكتبة أندرويد محلية، مستقلة، خفيفة الوزن، وفائقة السرعة لتحليل الأكواد، كشف الأخطاء، وإصلاحها بلغة Java و Kotlin. تُحزم كمكتبة أندرويد قياسية (`.AAR`) يمكن دمجها في أي تطبيق أندرويد خارجي دون dependence على التطبيق المضيف.

---

## 1. نظرة عامة على الهيكلية (Architecture Overview)

تتبع المكتبة الهندسة الطبقية (Layered Architecture) مع فصل تام بين:

| الطبقة | المسؤولية | الحزمة |
|--------|------------|-------|
| **واجهة البرمجة العامة (Public API)** | نقطة الدخول، إنشاء الجلسات، إلحاق المحرر، 설정ات الفحص | `com.kingid.codesdk.api` |
| **محرك التحليل (Core Engine)** | تنسيق المحللين، المدققات، والتخزين المؤقت | `com.kingid.codesdk.engine` |
| **التحليل والتزايد (Analysis)** | الفحص الكامل، الفحص التزايدي، Debounce، مستندات النصوص | `com.kingid.codesdk.analysis` |
| **المحلل اللغوي (Parser)** | تحويل النص المصدر إلى شجرة AST مبسطة (محول JavaParser/Kotlin اختياري) | `com.kingid.codesdk.parser` |
| **المدققات (Checkers)** | النحوي، الدلالي، الاستيرادات المفقودة، توافقية API | `com.kingid.codesdk.checker` |
| **الإصلاحات السريعة (Fixes)** | إنشاء اقتراحات الإصلاح (QuickFixes) لكل خطأ | `com.kingid.codesdk.fix` |
| **التخزين المؤقت (Cache)** | ذاكرة LRU في RAM + تخزين على القرص | `com.kingid.codesdk.cache` |
| **التشغيل متعدد الخيوط (Threading)** | Dispatchers.Default للخيوط الخلفية | `com.kingid.codesdk.threading` |
| **واجهة المحرر (Editor)** | تعميم يسمح بالالتصاق بأي محرر نصوص في التطبيق المضيف | `com.kingid.codesdk.editor` |
| **أدوات مساعدة (Util)** | Log، تحقق من المدخلات، ملفات وبصمات محتوى | `com.kingid.codesdk.util` |

مصدر الإلهام: بنية محللات الأكواد الداخلية ومكتبات الفحص والتحليل في مشروع **AndroidIDE** المفتوح المصدر (مراقبة المستندات عبر Document/Text Listeners، نموذج التشخيص Diagnostic، والإصلاحات السريعة Quick Fixes). تم إعادة بناء المحرك هنا كوحدة مستقلة خفيفة الوزن خالية من تبعيات IDE الثقيلة.

---

## 2. واجهة البرمجة العامة (Fluent Public API)

نقطة الدخولシングلتون `CodeAnalyzerSdk`:

```kotlin
// تهيئة واحدة عند启动 التطبيق المضيف
CodeAnalyzerSdk.initialize(context)

// إنشاء جلسة تحليل بإعدادات مخصصة (واجهة Fluent)
val analyzer = CodeAnalyzerSdk.initialize(context)
    .withConfig(projectConfig)
    .createSession()

// إلحاق محرر المحرر في التطبيق المضيف وتفعيل المراقبة اللحظية
analyzer
    .attachEditor(myCodeEditor)      // أي View ينفذ EditorAttachable
    .enableRealTime(true)
    .setDebounceMs(300)
    .start()
```

### dénomination سريع
- `CodeAnalyzerSdk.initialize(context)` — تُ Initialise مُ reckless الـ Dispatchers وتعيد منشئ عابر (`Initializer`).
- `withConfig(ProjectConfig)` — ضبط إعدادات الجلسة (مسارات المصادر، minSdk، 깊度 الفحص، مدة Debounce…).
- `createSession()` — إعادة `AnalyzerSession` جاهزة للعمل.
- `attachEditor(EditorAttachable)` — ربط المحرر بالمكتبة لالتقاط تغييرات النص.
- `enableRealTime(true)` / `setDebounceMs(ms)` — التحكم في الفحص اللحظي.
- `start()` / `stop()` — بدء/إيقاف حلقة المراقبة الخلفية.
- `analyzeFile(path)` / `analyzeProject()` / `analyzeSnippet(text, range, fileKey)` — تحليل حسب الطلب.
- `getQuickFixes(diagnostic)` / `applyFix(fix, callback)` — جلب وتطبيق الإصلاحات السريعة.

---

## 3. الفحص اللحظي الفائق وغير المتزامن (Real-time Async Analysis)

### 3.1 مراقبة التغييرات
يُنفذ التطبيق المضيف واجهة `EditorAttachable` وتُسجل المكتبة مستمع تغييرات عبر `setOnTextChangeListener { text, range -> ... }`. عند كل تعديل في المحرر، تُرسل المكتبة النص الجديد ونطاق التغيير إلى محرك التحليل.

### 3.2 خيوط خلفية + Debounce
- تُنفذ كل عمليات الفحص على `Dispatchers.Default` (خيوط خلفية مخصصة للعمليات التي تستهلك المعالج) بواسطة Kotlin Coroutines.
- تُستخدم آلية `Debouncer` (ت veio ذكية): تحتفظ بآخر تغيير فقط، وتُؤجل التنفيذ لمدّة قابلة للتكوين (افتراضياً 300 ميلي ثانية). إذا تغير النص снова خلال المدة، يتم إلغاء المهمة السابقة وبدء مهلة جديدة — مما يمنع الفحص المفرط ويضمن سلاسة الكتابة (لا تعليق في واجهة المستخدم).
- جميع callbacks (`onAnalysisCompleted`, `onFixesReady`, `onError`) تُعاد إلى خيط الواجهة الرئيسية (`Main Thread`) عبر `Handler(Looper.getMainLooper())`.

### 3.3 نموذج التدفق
```
تغيير نص في المحرر
      ↓
EditorAttachable.setOnTextChangeListener
      ↓
Debouncer.submit(text, range, fileKey)   ← يؤجل التنفيذ
      ↓ (بعد انقضاء المهلة بدون تغييرات جديدة)
CoroutineScope(Dispatchers.Default)
      ↓
AnalysisEngine.analyzeSnippet(...)
      ↓
mdققات (Syntax / Semantic / Imports / API)
      ↓
تخزين النتيجة في AnalysisCache
      ↓
النشر على Main Thread: onAnalysisCompleted + onFixesReady
```

---

## 4. الفحص الشامل والتزايدي (Full & Incremental AST Analysis)

### 4.1 Full Project Analysis (`FullProjectAnalyzer`)
يجول جميع جذور المصادر المحددة في `ProjectConfig.sourceRoots`, يحلل كل ملف源 via `CodeParser`, يجمع كل التشخيصات في `AnalysisResult` واحد. يُستخدم عند الفحص الأولي أو عند الطلب فـ Full Scan.

### 4.2 Incremental Analysis (`IncrementalAnalyzer`)
- يعيد فحص الملف الذي حدث فيه تغيير فقط، أو مقتطف نصي (snippet) من المحرر.
- يُستخدم **بصمة المحتوى (Content Fingerprint)** — تجزئة MD5 لنص الملف — تُخزن بجانب النتيجة في `AnalysisCache`. إذا وصل نفس النص بنفس البصمة، تُعاد النتيجة المخزنة مباشرة دون إعادة تحليل.
- يقلل استهلاك المعالج والذاكرة إلى أدنى حد للتعديلات الصغيرة المتكررة.

### 4.3 دعم التوافقية (API Level Compatibility)
مدقق `ApiCompatibilityChecker` يراقب:
- استخدام `@RequiresApi` بمستوى API أعلى من `minSdk` المحدد في الإعدادات → تحذير.
- واجهات/APIs 중요لة مدرجة في `deprecatedApiList` → تحذير مع موقع الاستدعاء.

---

## 5. كشف الأخطاء وتحليل الأكواد (Syntax, Semantic & Engine)

### 5.1 الأخطاء النحوية والتركيبية (`SyntaxChecker`)
يعتمد على `Tokenizer` مدمج خفيف الوزن للكشف عن:
- توازن الأقواس `{ } ( ) [ ]`.
- الأقواس غير المغلقة والسلاسل النصية غير المنتهية.
- producen `Diagnostic` بمستوى `Severity.ERROR` وموقع دقيق (سطر، عمود، أوفست).

### 5.2 التحقق من الأنواع والاستيرادات (`SemanticChecker`, `ImportResolver`)
- `SemanticChecker`: بناء جدول رموز بسيط للكيانات المعلنة (class, fun, val, var) والإبلاغ عن الرموز المستخدمة بدون إعلان.
- `ImportResolver`: استخراج عبارات `import` ومقارنتها بفهرس الرموز المعروف (`knownSymbols`)، والإبلاغ عن الاستيرادات المفقودة مع اقتراح المسار الكامل (Fully Qualified Name).

### 5.3 نموذج التشخيص (Diagnostic Model)
كل خطأ يُمثّل بـ `Diagnostic` يحوي:
- `code`: رمز الخطأ.
- `message`: رسالة-readable.
- `severity`: ERROR / WARNING / INFO / HINT.
- `location`: DiagnosticLocation (file, startLine/Col, endLine/Col, offsets).
- `ruleId`: معرف القاعدة المُنفذة.
- `fixable`: هل يدعم إصلاحاً سريعاً.
- `data`: بيانات إضافية (مثل اسم الرمز والاستيراد المقترح).

---

## 6. نظام الإصلاحات السريعة (Quick Fixes & Intention Actions)

`FixEngine` يستقبل `Diagnostic` ويعيد قائمة `List<QuickFix>`:

| الخطأ (ruleId) | الإصلاح المقترح (QuickFix) |
|----------------|---------------------------|
| `missing-import` | `fix-add-import-<symbol>`: إضافة `import <path>;` في أعلى الملف |
| `undeclared-variable` | `fix-declare-<symbol>`: اقتراح إعلان `val <symbol> = null` |

تحتوي كل `QuickFix` على:
- `id`, `title`, `description` للعرض في واجهة التطبيق المضيف.
- `edits`: قائمة تعديلات نصية (`TextEdit`) تُطبق على المحرر.
- `requiresUserConfirmation`: क्या يتطلب موافقة المستخدم قبل التطبيق.

يتولى التطبيق المضيف تنفيذ `EditorAttachable.applyFix(fix, callback)` لتطبيق التعديل على محوره الخاص.

---

## 7. هندسة الأداء وإدارة الذاكرة

### 7.1 منع تسرب الذاكرة
- 모든 كائنات الجلسات والتحليلات تُنشأ ضمن `CoroutineScope` يُلغى عند `stop()` / `detachEditor()`.
- `Debouncer` يلغي جميع المهام المؤجلة عند الإلغاء (`cancel()`).
- لا تُحتفظ بمراجع للسياق (Context) إلا `applicationContext` لتسريب Activity.
- `AnalysisCache` محدودة بعدد entrées افتراضياً 500 (قابل الضبط عبر `maxCacheEntries`)، وتُطبّق سياسة إزالة الأقدم استخداماً (LRU) في `MemoryCache` لمنع `OutOfMemoryError`.

### 7.2 نظام التخزين المؤقت المزدوج
- `MemoryCache`: LinkedHashMap LRU في الذاكرة للوصول فائق السرعة.
- `DiskCache` (اختياري): تسلسل نتائج الفحص إلى ملفات في مجلد cache الخاص بالتطبيق لاستعادة النتائج بعد إعادة تشغيل العملية.
- مفتاح التخزين = مسار الملف + بصمة MD5 لمحتوى الملف → ضمان تجديد النتيجة فقط عند التغيير الفعلي.

### 7.3 حجم المكتبة (AAR Size)
- لا توجد تبعيات إلزامية الثقيلة (لا JavaParser، لا مترجم Kotlin، لا محركات AST خارجية).
- المحلل الافتراضي مدمج (`Tokenizer` + فحص أساسي) ليعمل الـ SDK فوراً.
- يمكن إضافة `JavaParserAdapter` / `KotlinParserAdapter` عند الحاجة كمكتبات اختيارية في التطبيق المضيف.

---

## 8. نموذج الحزم (Modularization & AAR)

```
kingid/                          جذر المستودع
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── code-analyzer-sdk/           وحدة المكتبة (تُنتج AAR)
│   ├── build.gradle.kts         (com.android.library + kotlin-android)
│   ├── proguard-rules.pro
│   ├── consumer-rules.pro       (تُطبق على التطبيق المضيف)
│   ├── src/main/AndroidManifest.xml
│   └── src/main/java/com/kingid/codesdk/...
│       ├── api/                 (CodeAnalyzerSdk, AnalyzerSession, ProjectConfig, …)
│       ├── engine/              (AnalysisEngine)
│       ├── analysis/            (Debouncer, FullProjectAnalyzer, IncrementalAnalyzer, …)
│       ├── parser/              (CodeParser, AstNode, Tokenizer, Adapters)
│       ├── checker/             (SyntaxChecker, SemanticChecker, ImportResolver, ApiCompatibilityChecker, TypeResolver)
│       ├── fix/                 (FixEngine, AddImportFix, UndeclaredVariableFix)
│       ├── cache/               (AnalysisCache, MemoryCache, DiskCache)
│       ├── threading/           (AnalysisDispatcher)
│       ├── editor/              (HostEditorAdapter)
│       └── util/                (Logger, Preconditions, FileExtensions)
│   └── src/main/res/            (قيم، سمات، معرفة موارد قليلة جداً)
└── app/                         تطبيق تجريبي لاختبار الدمج
```

---

## 9. амн всех المتطلبات (Coverage Matrix)

| المتطلب | التحقيق |
|----------|---------|
| فصل المنطق عن واجهة المستخدم | طبقات `api` vs `core/engine/checker` مع واجهة `EditorAttachable` للتطبيق المضيف |
| واجهة برمجة عامة ميسرة (Fluent API) | `CodeAnalyzerSdk.initialize().withConfig().createSession().attachEditor()` |
| الفحص اللحظي غير المتزامن | `Dispatchers.Default` + `Debouncer` + نُشر على Main Thread |
| الفحص الشامل والتزايدي | `FullProjectAnalyzer` و `IncrementalAnalyzer` مع بصمة محتوى |
| التحقق من توافقية API والإهمال | `ApiCompatibilityChecker` (@RequiresApi، deprecated list) |
| الأخطاء النحوية والتركيبية | `SyntaxChecker` عبر `Tokenizer` |
| الأنواع، الاستيرادات المفقودة والاقتراح | `SemanticChecker`، `ImportResolver` |
| الإصلاحات السريعة | `FixEngine` + `QuickFix` + `TextEdit` |
| إدارة الذاكرة ومنع التسريب | LRU cache، إلغاء الكوروتينات، applicationContext فقط |
| التخزين المؤقت الذكي | `MemoryCache` + `DiskCache` ببصمة MD5 |
| حزمة AAR مستقلة | وحدة `com.android.library` بدون مكونات نظام (لا Activities/Services) |

---

## 10. ملاحظات حول AndroidIDE

المكتبة مستوحاة من مفاهيم مشروع AndroidIDE (مراقبة المستندات، نموذج التشخيص، MDC/الإصلاحات السريعة، وتحليل الأكواد التزايدي). kode المصدري هنا كُُتب من zéro ليكون مكتبة مستقلة (.AAR) دون تبعية على كود AndroidIDE مباشرة. يمكن في الإصدارات القادمة استبدال `Tokenizer` بمحلل AST حقيقي من JavaParser (لـ Java) أو مترجم Kotlin (لـ Kotlin) عبر المحولات الموجودة في حزمة `parser`.
