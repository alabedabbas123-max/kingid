package com.kingid.codesdk.core

/**
 * مستوى خطورة التشخيص (Diagnostic severity).
 * مستوحى من تصنيفات المشكلات في أدوات التحليل الثابتة (مثل AndroidIDE Analyzer و Lint).
 */
enum class Severity(val level: Int, val label: String) {
    ERROR(4, "خطأ"),
    WARNING(3, "تحذير"),
    INFO(2, "معلومة"),
    HINT(1, "تنبيه")
}
