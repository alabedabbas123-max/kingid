# التقرير الشامل لنظام محرر DEX Studio

## ملاحظة منهجية

هذا التقرير يصف نظام محرر احترافي مقترح ومتوافق مع بنية التطبيق التجريبية الحالية، ويحدد مواصفات يمكن تنفيذها لبناء نظام مشابه. لا يمكن اعتباره تفريغًا داخليًا لتطبيق تجاري مغلق أو تأكيدًا لخصائص غير موجودة في النسخة الحالية. النسخة الحالية تحتوي على واجهة Compose وبنية محرك أولية، أما هذا المستند فهو المواصفات التنفيذية الكاملة للمحرر المطلوب.

---

# 1. أهداف المحرر

يجب أن يحقق المحرر الآتي:

- فتح ملفات Smali وDEX metadata وJSON وCSV وTXT.
- التنقل السريع بين آلاف أو ملايين الأسطر.
- عدم تجميد واجهة المستخدم أثناء القراءة أو الحفظ أو التحليل.
- تمييز لغوي سريع ومناسب لـ Smali.
- إكمال تلقائي يعتمد على السياق.
- عرض أخطاء وتحذيرات أثناء الكتابة.
- البحث داخل الملف أو المشروع بالكامل.
- التنقل بين التعريفات والاستدعاءات.
- حفظ تلقائي آمن وقابل للاستعادة.
- دعم الملفات الكبيرة دون نسخها بالكامل عدة مرات في الذاكرة.
- التكامل مع شجرة المشروع وفهرس DEX العالمي.

---

# 2. البنية العامة

```text
EditorScreen
├── EditorTabs
├── ProjectTree
├── CodeViewport
├── Minimap اختياري
├── ProblemsPanel
├── SearchPanel
├── StatusBar
└── CompletionPopup

Editor Core
├── DocumentManager
├── PieceTable/Rope Buffer
├── UndoRedoManager
├── SyntaxTokenizer
├── IncrementalParser
├── CompletionEngine
├── DiagnosticsEngine
├── SymbolNavigator
├── SaveManager
└── WorkspaceIndex
```

يفضل فصل المحرر إلى طبقتين:

```text
UI Layer       Compose وعمليات الإدخال والرسم
Editor Core    نص، مؤشرات، تحليل، حفظ، فهرسة
```

ولا ينبغي وضع parsing أو البحث أو التخزين داخل Composable مباشرة.

---

# 3. نموذج الوثيقة Document Model

## 3.1 Piece Table أو Rope

لا يفضل استخدام `String` واحد وإعادة إنشائه عند كل ضغطة مفتاح. ذلك يؤدي إلى نسخ كبير للنص وتوقف الواجهة.

الخيارات المناسبة:

- Piece Table: مناسب للمحررات التي تحتوي على تعديلات متكررة.
- Rope: مناسب للنصوص الكبيرة والتعديلات الموضعية.
- Gap Buffer: جيد لملفات متوسطة الحجم ومؤشر واحد.

الاختيار المقترح هو Piece Table أو Rope مع فهرس أسطر منفصل.

```kotlin
interface TextDocument {
    val uri: String
    val length: Long
    fun readRange(start: Long, end: Long): CharSequence
    fun insert(offset: Long, text: CharSequence)
    fun delete(start: Long, end: Long)
    fun lineOf(offset: Long): Int
    fun offsetOfLine(line: Int): Long
}
```

## 3.2 Line Index

يتم الاحتفاظ بمواقع بدايات الأسطر في بنية قابلة للتحديث:

```text
lineStarts = [0, 42, 87, 131, ...]
```

عند إدخال نص، لا يعاد حساب الملف كاملًا؛ يعاد حساب الجزء المتأثر فقط، ثم تضاف الإزاحة إلى بقية الفهرس باستخدام Fenwick Tree أو بنية مشابهة عند الملفات الضخمة.

الفوائد:

- انتقال مباشر إلى رقم سطر.
- حساب رقم السطر من موضع المؤشر بسرعة.
- تفعيل Go to line دون قراءة الملف كاملًا.
- رسم أرقام الأسطر للمنطقة المرئية فقط.

---

# 4. الرسم والأداء

## 4.1 Virtualized Viewport

لا يرسم المحرر كل الأسطر. يرسم فقط:

```text
visibleStartLine - overscan
visibleEndLine + overscan
```

مثال:

```text
الأسطر الظاهرة: 400–450
الأسطر الاحتياطية: 390–460
```

عند التمرير، يعاد رسم المنطقة الظاهرة فقط.

## 4.2 عدم استخدام LazyColumn لكل حرف

يمكن استخدام `LazyColumn` للنموذج الأولي، لكنه ليس مثاليًا لمحرر نصوص كامل، لأن كل سطر قد يصبح عنصر Compose منفصلًا وتزداد كلفة إعادة التركيب.

الأفضل:

- Custom Canvas للرسم.
- Text layout محدود للمنطقة المرئية.
- أو محرر Native مخصص مع virtual viewport.

## 4.3 خيوط التنفيذ

```text
Main/UI Thread
└── الإدخال، المؤشر، الرسم، التحديد

Editor Worker
└── tokenization، line index، completion

Analysis Worker
└── parser، diagnostics، call graph

IO Dispatcher
└── القراءة، الحفظ، ZIP، التقارير
```

قاعدة مهمة: لا ينفذ parsing أو ZIP أو حفظ كامل على Main Thread.

---

# 5. التمييز اللغوي Syntax Highlighting

## 5.1 Tokenizer لـ Smali

يجب التعرف على:

- التعليقات التي تبدأ بـ `#`.
- directives مثل `.class`, `.method`, `.field`, `.end method`.
- labels مثل `:cond_0`.
- registers مثل `v0`, `p1`.
- opcodes مثل `invoke-virtual`, `move-result`, `return-void`.
- descriptors مثل `Ljava/lang/String;`.
- النصوص والسلاسل.
- الأرقام.
- annotations.
- أسماء classes وmethods وfields.

```kotlin
enum class TokenKind {
    COMMENT, DIRECTIVE, OPCODE, REGISTER, LABEL,
    TYPE_DESCRIPTOR, STRING, NUMBER, METHOD_NAME,
    FIELD_NAME, ANNOTATION, ERROR, PLAIN
}

data class Token(
    val start: Int,
    val end: Int,
    val kind: TokenKind
)
```

## 5.2 Incremental Tokenization

عند تعديل سطر واحد:

1. يعاد تحليل السطر المعدل.
2. يعاد تحليل السطر التالي إذا تأثر السياق.
3. لا يعاد تحليل الملف كاملًا إلا عند تغير block كبير.
4. يتم تخزين tokens في cache حسب رقم السطر ونسخة الوثيقة.

```text
Document Version 10
Line 200 → tokens cache version 10
بعد التعديل:
Document Version 11
يعاد تحليل الأسطر 200–202 فقط
```

## 5.3 الألوان

يجب أن تأتي الألوان من Theme وليس من tokenizer:

```kotlin
data class EditorColors(
    val background: Color,
    val foreground: Color,
    val keyword: Color,
    val opcode: Color,
    val comment: Color,
    val string: Color,
    val register: Color,
    val error: Color
)
```

وهذا يسمح بالتبديل بين Light وDark دون إعادة بناء النص.

---

# 6. التنقل بين الأسطر

## الخصائص

- تمرير عمودي سريع.
- تمرير أفقي عند الأسطر الطويلة.
- تثبيت أرقام الأسطر.
- Go to line.
- Go to symbol.
- Go to definition.
- Back/Forward history.
- قفز إلى بداية أو نهاية block.
- طي methods وclasses.
- الحفاظ على موضع المؤشر لكل تبويب.
- استعادة موضع آخر جلسة.

## التعامل مع الأسطر الطويلة

لا يتم layout للنص الكامل إذا كان أطول من حد معين. يقسم إلى مقاطع مرئية، مع دعم horizontal scrolling. كما يمكن عرض حد أقصى في الذاكرة للـ layout ثم تحريره عند الخروج من الشاشة.

## تكرار المؤشر

يتم تخزين:

```kotlin
data class EditorPosition(
    val line: Int,
    val column: Int,
    val scrollX: Int,
    val scrollY: Int,
    val selectionStart: Long? = null,
    val selectionEnd: Long? = null
)
```

---

# 7. شجرة المشروع

## 7.1 نموذج الملفات

```kotlin
sealed class ProjectNode {
    data class Directory(
        val path: String,
        val children: List<ProjectNode>,
        val expanded: Boolean
    ) : ProjectNode()

    data class FileNode(
        val path: String,
        val type: FileType,
        val size: Long,
        val modifiedAt: Long
    ) : ProjectNode()
}
```

## 7.2 Lazy Tree

لا يتم تحميل كل الملفات دفعة واحدة. يتم تحميل أبناء المجلد عند فتحه:

```text
workspace/
├── input/       لا تقرأ محتوياته حتى يفتح المستخدم الملف
├── smali/       children lazy
├── output/      children lazy
└── reports/     children lazy
```

## 7.3 أنواع العقد

- DEX.
- Smali.
- Java/Kotlin إن وجدت.
- JSON.
- XML.
- CSV.
- Directory.
- Report.
- Binary/Unknown.

## 7.4 مزامنة الشجرة

تستخدم File Watcher أو فحصًا دوريًا منخفض التكلفة:

- لا يعاد بناء الشجرة كاملة بعد كل تغيير.
- يحدث المجلد المتأثر فقط.
- الملفات المفتوحة لا تغلق عند تغيرها خارجيًا.
- يظهر تنبيه: “تم تعديل الملف خارج المحرر”.
- يختار المستخدم Merge أو Reload أو Keep Local.

---

# 8. التبويبات وإدارة الملفات المفتوحة

كل تبويب يحتفظ بـ:

```kotlin
data class OpenDocument(
    val path: String,
    val documentId: String,
    val dirty: Boolean,
    val position: EditorPosition,
    val readOnly: Boolean,
    val encoding: Charset,
    val lineEnding: LineEnding
)
```

الخصائص:

- فتح عدة ملفات.
- تبويب نشط واحد.
- علامة `*` للملف غير المحفوظ.
- إغلاق مع طلب حفظ.
- Restore session.
- حد اختياري للتبويبات المفتوحة.
- تفريغ محتوى تبويب غير نشط من الذاكرة مع إبقاء metadata.

---

# 9. الحفظ والتخزين

## 9.1 التخزين السريع

التدفق المقترح:

```text
Editor Buffer
   ↓ debounce 500–1000ms
Save Queue
   ↓ Dispatchers.IO
Temporary File
   ↓ fsync اختياري
Atomic Rename
   ↓
Final File
```

لا يكتب المحرر فوق الملف مباشرة. يتم إنشاء:

```text
file.smali.tmp
```

ثم التحقق من الحجم، ثم النقل إلى الملف الأصلي.

## 9.2 Auto-save

- حفظ تلقائي بعد فترة خمول.
- حفظ عند انتقال التطبيق إلى الخلفية.
- حفظ نسخة recovery في مساحة التطبيق.
- حذف recovery بعد الحفظ الناجح.
- الاحتفاظ بعدة إصدارات قصيرة عند المشاريع المهمة.

```kotlin
data class SavePolicy(
    val debounceMs: Long = 750,
    val createRecoveryCopy: Boolean = true,
    val maxRecoveryCopies: Int = 5
)
```

## 9.3 Encoding وLine Ending

يجب اكتشاف:

- UTF-8.
- UTF-8 BOM.
- UTF-16 عند الحاجة.
- LF.
- CRLF.

ويجب عدم تغيير line ending دون طلب المستخدم، لأن ذلك يسبب تغييرات ضخمة في Git diff.

---

# 10. فحص الجمل Diagnostics

## 10.1 مستويات الفحص

```kotlin
enum class Severity { INFO, WARNING, ERROR }

data class Diagnostic(
    val file: String,
    val line: Int,
    val column: Int,
    val endColumn: Int,
    val severity: Severity,
    val code: String,
    val message: String,
    val quickFix: QuickFix? = null
)
```

## 10.2 أمثلة Smali

- register غير معرف.
- register خارج النطاق.
- opcode لا يناسب نوع operands.
- عدد المعاملات غير صحيح.
- method descriptor غير صالح.
- label غير موجود.
- label مكرر.
- `.end method` مفقود.
- `.locals` غير متوافق.
- register parameter غير صحيح.
- type descriptor غير معروف.
- استدعاء method غير موجود في الفهرس.
- field reference غير موجود.

## 10.3 التشخيص التدريجي

لا ينتظر المحرر اكتمال الملف. أثناء الكتابة يتم تقديم diagnostics جزئية، ويعاد الفحص بعد debounce:

```text
كتابة → 150ms debounce → parse current method → update diagnostics
```

إذا كان الملف ضخمًا، يقتصر الفحص على method الحالية والـ references المتأثرة.

---

# 11. الإكمال التلقائي

## 11.1 مصادر الإكمال

- Opcodes.
- Directives.
- Registers المستخدمة في method الحالية.
- Labels في block الحالية.
- Classes في Global Symbol Index.
- Methods حسب owner والـ descriptor.
- Fields حسب النوع.
- Snippets شائعة.
- أسماء من قاموس المشروع.

## 11.2 ترتيب النتائج

```text
1. تطابق prefix
2. نفس method أو class
3. الرموز المستخدمة حديثًا
4. الرموز الموجودة في نفس package
5. الرموز العامة
6. الترتيب الأبجدي
```

## 11.3 الأداء

- لا يفتح popup قبل وجود حرفين عند البحث العالمي.
- يتم استخدام Trie أو Fuzzy index.
- يتم حساب النتائج في worker.
- يتم إلغاء الطلب السابق عند كل حرف جديد.
- لا يتجاوز عدد النتائج المرئية 30–50.
- cache للإكمال حسب prefix وDocumentVersion.

```kotlin
class CompletionEngine(private val index: WorkspaceIndex) {
    suspend fun complete(context: CompletionContext): List<CompletionItem> =
        withContext(Dispatchers.Default) {
            index.lookup(context.prefix, limit = 50)
        }
}
```

---

# 12. البحث والاستبدال

يجب دعم:

- بحث داخل الملف.
- بحث في المشروع.
- Regex اختياري.
- Case sensitive.
- Whole word.
- Preview قبل الاستبدال.
- Replace one.
- Replace all مع Undo واحد.
- بحث في Smali descriptors.
- بحث في references من الفهرس وليس النص فقط.

البحث داخل المشروع ينفذ عبر:

```text
File Index
  → candidate files
  → chunked scan
  → result stream
  → UI pagination
```

ولا يجب تحميل كل النتائج دفعة واحدة.

---

# 13. Go to Definition وCross References

يحتاج المحرر إلى فهرس موحد:

```kotlin
interface WorkspaceIndex {
    suspend fun findDefinition(symbol: SymbolId): Location?
    suspend fun findReferences(symbol: SymbolId): List<Location>
    suspend fun searchClasses(prefix: String): List<SymbolId>
    suspend fun update(file: File)
}
```

عند الضغط على اسم method:

1. استخراج owner/name/descriptor.
2. البحث في Global Symbol Index.
3. فتح الملف المناسب.
4. الانتقال إلى السطر.
5. إضافة الموقع إلى history.

إذا كان الرمز dynamic أو Reflection، يجب إظهار:

```text
تعذر تحديد المرجع بشكل مؤكد — قد يكون استدعاءً ديناميكيًا
```

---

# 14. سجل التراجع والإعادة

لا تخزن نسخة كاملة من الملف لكل عملية. استخدم Command Pattern:

```kotlin
interface EditCommand {
    fun apply(document: TextDocument)
    fun undo(document: TextDocument)
}
```

ويتم تجميع الكتابة السريعة في transaction واحدة:

```text
كل الحروف في نفس الجملة خلال 300ms → عملية Undo واحدة
```

عند تنفيذ Rename أو Replace All، يتم إنشاء transaction واحدة قابلة للإلغاء بالكامل.

---

# 15. أمان المحرر

- فتح الملفات داخل مساحة المشروع أو URI يختاره المستخدم.
- عدم تنفيذ أي كود DEX.
- عدم تفسير أو تحميل classes من المشروع.
- حد أقصى لحجم الملف القابل للتحرير.
- فتح الملفات الثنائية في Viewer للقراءة فقط.
- منع path traversal في ZIP.
- منع الكتابة خارج workspace.
- تأكيد قبل الحذف أو التنظيف.
- سجل لكل عملية تعديل.
- زر Restore للنسخة السابقة.

---

# 16. إدارة الذاكرة

سياسة مقترحة:

```text
< 2 MB       تحرير كامل
2–20 MB      تحرير كامل مع tokenization جزئي
20–100 MB    viewport + parsing متدرج
> 100 MB     قراءة/بحث، والتحرير اختياري أو مقيد
```

يجب استخدام:

- `ByteBuffer` أو streams للقراءة.
- Cache محدود الحجم.
- LRU cache للـ tokens والـ layouts.
- تحرير cache عند `onTrimMemory`.
- عدم الاحتفاظ بنسخ String متعددة.

---

# 17. التفاعل مع Compose

يجب أن يكون `ViewModel` هو مصدر الحالة:

```kotlin
data class EditorUiState(
    val activeFile: String? = null,
    val visibleLines: IntRange = 0..0,
    val diagnostics: List<Diagnostic> = emptyList(),
    val completion: List<CompletionItem> = emptyList(),
    val isSaving: Boolean = false,
    val dirty: Boolean = false
)
```

الأحداث:

```kotlin
sealed interface EditorEvent {
    data class Open(val path: String): EditorEvent
    data class Insert(val offset: Long, val text: String): EditorEvent
    data class Delete(val start: Long, val end: Long): EditorEvent
    data object Save: EditorEvent
    data object Undo: EditorEvent
}
```

ولا يجب تخزين النص الكامل داخل `mutableStateOf(String)` للملفات الكبيرة.

---

# 18. دورة تحديث المحرر

```text
Key event
  ↓
Apply edit to PieceTable
  ↓
Update line index locally
  ↓
Update cursor immediately
  ↓
Invalidate visible lines
  ↓
Debounce tokenizer
  ↓
Update completion
  ↓
Run diagnostics for affected method
  ↓
Schedule autosave
```

زمن مستهدف على جهاز متوسط:

- استجابة المؤشر: أقل من 16ms.
- إدخال حرف: أقل من 16–32ms بصريًا.
- إظهار الإكمال: أقل من 100ms.
- تشخيص method صغيرة: أقل من 250ms.
- فتح ملف متوسط: أقل من ثانية تقريبًا.

---

# 19. الاختبارات المطلوبة

## Unit Tests

- Line index.
- Piece table.
- Undo/redo.
- Tokenizer.
- Descriptor parser.
- Completion ranking.
- ZIP path validation.
- Atomic save.

## Property Tests

- تطبيق edit ثم undo يعيد النص الأصلي.
- تطبيق rename ثم reverse mapping يعيد النموذج الأصلي.
- لا ينتج path خارج workspace.
- لا يضيع أي حرف بعد save/reload.

## Performance Tests

- فتح ملف Smali كبير.
- تمرير سريع من البداية للنهاية.
- Replace All على آلاف النتائج.
- فتح 50 تبويبًا.
- فهرسة مشروع Multi-DEX.
- استهلاك الذاكرة أثناء التمرير.

## Golden Tests

حفظ مخرجات tokenizer وformatter المتوقعة لعينات Smali ثابتة، ثم مقارنة أي تغيير في المحرك بها.

---

# 20. الخصائص النهائية للمحرر

- Tabs متعددة.
- حفظ تلقائي.
- Recovery.
- Undo/Redo متعدد المستويات.
- Syntax highlighting.
- Code folding.
- Line numbers.
- Current line highlight.
- Search/Replace.
- Regex.
- Go to line.
- Go to definition.
- Find references.
- Diagnostics.
- Quick fixes.
- Completion.
- Snippets.
- Project tree lazy loading.
- Multi-DEX global index.
- Read-only binary viewer.
- Dark/Light themes.
- Font scaling.
- Horizontal/vertical scrolling.
- Minimap اختياري.
- Copy path وdescriptor.
- Export selection.
- Compare before/after.
- Git-friendly line endings.
- Atomic save.
- Rollback.
- Progress indicators.
- Cancellation.
- سجل تغييرات CSV.
- تقارير JSON.

---

# 21. الخلاصة التنفيذية

لجعل المحرر سلسًا وسريعًا، يجب تجنب ثلاثة أخطاء رئيسية:

1. تخزين الملف الكبير في `String` وإعادة بنائه مع كل حرف.
2. رسم كل أسطر الملف في نفس الوقت.
3. تنفيذ parsing أو البحث أو الحفظ داخل Main Thread.

النهج الأفضل هو:

```text
Piece Table/Rope
+ Line Index
+ Virtualized Viewport
+ Incremental Tokenizer
+ Background Diagnostics
+ Indexed Completion
+ Lazy Project Tree
+ Debounced Atomic Save
+ Global Symbol Index
+ Undo Transactions
```

بهذه البنية يمكن بناء محرر Smali/Dex سريع وقابل للتوسع على الهواتف، مع الحفاظ على سلاسة الواجهة حتى مع مشاريع Multi-DEX كبيرة.
