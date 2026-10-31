package com.kingid.codesdk.core

/**
 * تشخيص واحد (مشكلة مكتشفة) يُنتج عن محرك الفحص.
 * يحتوي على رمز الخطأ، الرسالة، مستوى الخطورة، الموقع، وقابلة للإصلاح أم لا.
 */
data class Diagnostic(
    val code: String,
    val message: String,
    val severity: Severity,
    val location: DiagnosticLocation,
    val ruleId: String,
    val fixable: Boolean = false,
    val data: Map<String, Any> = emptyMap()
) {
    fun isError() = severity == Severity.ERROR
    fun isWarning() = severity == Severity.WARNING
}
