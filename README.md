# kingid — KingID Code Analyzer SDK

مكتبة أندرويد محلية، مستقلة، خفيفة الوزن، وفائقة السرعة (Standalone & High-Performance SDK)
لتحليل الأكواد، فحص الأخطاء، وإصلاحها بلغة Java و Kotlin. تُحزم كمكتبة أندرويد قياسية (`.AAR`)
يمكن دمجها في أي تطبيق أندرويد خارجي بسهولة.

## مكونات المستودع

| المسار | المحتوى |
|--------|---------|
| `code-analyzer-sdk/` | وحدة المكتبة التي تنتج ملف `code-analyzer-sdk-release.aar` |
| `app/` | تطبيق تجريبي يُظهر دمج المكتبة واستدعاء محركات الفحص |
| `DESIGN_AR.md` | التصميم الهندسي التفصيلي (البنية، الطبقات، واجهة برمجة عامة، الفحص اللحظي، التزايدي، الإصلاحات السريعة، إدارة الذاكرة، التخزين المؤقت) |
| `BUILD_AR.md` | خطوات حزم المكتبة وبنائها كملف AAR محلي ودمجها في تطبيقات خارجية |
| `INTEGRATION_AR.md` | دليل الدمج والاستدعاء: التهيئة، الإعدادات، إلحاق المحرر، عرض النتائج وتطبيق الإصلاحات |

## مثال الاستخدام السريع

```kotlin
// 1. تهيئة المكتبة مرة واحدة
CodeAnalyzerSdk.initialize(context)

// 2. إنشاء جلسة تحليل وإلحاق المحرر
val analyzer = CodeAnalyzerSdk.initialize(context)
    .withConfig(projectConfig)
    .createSession()
    .attachEditor(myCodeEditor)
    .enableRealTime(true)
    .setDebounceMs(300)
    .start()

// 3. فحص مقتطف نصي (خلفي، غير متوقف)
val result = analyzer.analyzeSnippet(editorText, null, "<editor>")

// 4. جلب وتطبيق الإصلاحات السريعة
val fixes = analyzer.getQuickFixes(diagnostic)
analyzer.applyFix(fixes.first()) { /* تم التطبيق */ }
```

## البناء

```bash
./gradlew :code-analyzer-sdk:assembleRelease
```

ملف AAR الناتج:
`code-analyzer-sdk/build/outputs/aar/code-analyzer-sdk-release.aar`

التفاصيل الكاملة في ملفات `BUILD_AR.md` و `INTEGRATION_AR.md`.

## الترخيص

غير محدد — للاستخدام الداخلي والتقييم.
