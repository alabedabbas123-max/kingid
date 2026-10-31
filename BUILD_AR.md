# KingID Code Analyzer SDK — خطوات حزم المكتبة وبنائها كملف AAR محلي

يفترض هذا الدليل وجود Android Studio أو Gradle مع SDK أندرويد مثبت. البيئة المستخدمة في هذا المستودع لا تحتوي على JDK/Gradle محلياً، لذلك يتم التنفيذ كملفات مصدرية وتُبنى عبر Android Studio.

---

## المتطلبات المسبقة

- JDK 8 أو أعلى (يفضل JDK 11+)
- Android SDK (compileSdk 34، minSdk 21)
- Android Studio Giraffe / Flamingo / است جديد (لإدارة Gradle تلقائياً)
- اتصال إنترنت لتحميل محارف Gradle ومكتبات AndroidX/Kotlin (في أول بناء)

---

## هيكل الملفات للبناء

```
code-analyzer-sdk/build.gradle.kts   # وحدة المكتبة (com.android.library)
app/build.gradle.kts                  # تطبيق تجريبي (للتحقق فقط)
settings.gradle.kts                   # يدرج وحدتي :code-analyzer-sdk و :app
gradle.properties
```

---

## خطوات البناء (via Android Studio)

1. افتح المجلد `/home/user/kingid` كمشروع في Android Studio (`File → Open`).
2. سيُطلب من Android Studio تحميل محارف Gradle (Gradle Wrapper) — انتظر اكتمال المزامنة (Sync).
3. تأكد من تحديد وحدة `code-analyzer-sdk` في شريط الأدوات (Build Variant).
4. من القائمة اختر: **Build → Make Project** أو من الطرفية:

   ```bash
   ./gradlew :code-analyzer-sdk:assembleRelease
   ```

   *(إذا لم يكن `gradlew` موجوداً، نفذ `gradle wrapper` مرة واحدة أو دع Android Studio يولده تلقائياً.)*

5. ملف AAR الناتج سيكون في المسار:

   ```
   code-analyzer-sdk/build/outputs/aar/code-analyzer-sdk-release.aar
   ```

---

## خطوات البناء (via سطر الأوامر، إذا توفر Gradle)

```bash
cd /home/user/kingid
# توليد wrapper إن لم يكن موجوداً
gradle wrapper --gradle-version 8.2

# بناء AAR للإصدار النهائي
./gradlew :code-analyzer-sdk:assembleRelease

# بناء إصدار التصحيح
./gradlew :code-analyzer-sdk:assembleDebug

# تنظيف المخرجات
./gradlew :code-analyzer-sdk:clean
```

---

## دمج AAR في تطبيق أندرويد خارجي

الطريقتان التاليتان متكافئتان:

### الطريقة أ: كوحدة مكتبة محلية (مُفضّل للتطوير المشترك)
انسخ مجلد `code-analyzer-sdk` إلى مشروعك، ثم في `settings.gradle.kts`:

```kotlin
include(":code-analyzer-sdk")
project(":code-analyzer-sdk").projectDir = file("libs/code-analyzer-sdk")
```

في `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":code-analyzer-sdk"))
}
```

### الطريقة ب: كملف AAR جاهز
انسخ الملف `code-analyzer-sdk-release.aar` إلى مجلد `app/libs`، ثم في `app/build.gradle.kts`:

```kotlin
android {
    repositories {
        flatDir {
            dirs("libs")
        }
    }
}

dependencies {
    implementation(name: "code-analyzer-sdk-release", ext: "aar")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("androidx.annotation:annotation:1.9.0")
    implementation("androidx.core:core-ktx:1.12.0")
}
```

---

## قواعد ProGuard للملف AAR

ملف `consumer-rules.pro` يُرفق تلقائياً مع الـ AAR ويُطبق على التطبيق المضيف:

```proguard
-keep class com.kingid.codesdk.api.CodeAnalyzerSdk { *; }
-keepclassmembers class com.kingid.codesdk.api.** {
    public *;
}
```

---

## ملحقات اختيارية (تحليل AST حقيقي)

للحصول على تحليل AST كامل ودقيق بدلاً من المحلل الخفيف المدمج:

- **Java**: أضف `implementation("com.github.javaparser:javaparser-core:3.25.0")` في التطبيق المضيف، ثم نفّذ `com.kingid.codesdk.parser.CodeParser` باستخدام JavaParser وسجلّه في `AnalysisEngine.parsers`.
- **Kotlin**: أضف تبعية `org.jetbrains.kotlin:kotlin-compiler` واستخرج شجرة `KtFile` عبر `KotlinCompiler` وقم بتحويلها إلى `AstNode`.

تترك هذه الملحقات خارج الـ AAR الأساسي للحفاظ على خفة الوزن وسرعة التحميل.
