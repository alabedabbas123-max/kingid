package com.kingid.codesdk.core

/**
 * موقع الخطأ داخل الملف المصدر (سطر، عمود، ومنزلقات الأوفست للتعامل المباشر مع نص المحرر).
 */
data class DiagnosticLocation(
    val file: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val startOffset: Int = -1,
    val endOffset: Int = -1
) {
    fun length() = endOffset.coerceAtLeast(startOffset) - startOffset.coerceAtLeast(0)
}
