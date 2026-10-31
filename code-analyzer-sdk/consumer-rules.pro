# Consumer ProGuard rules for the AAR (apply to the host app)
-keep class com.kingid.codesdk.api.CodeAnalyzerSdk { *; }
-keepclassmembers class com.kingid.codesdk.api.** {
    public *;
}
