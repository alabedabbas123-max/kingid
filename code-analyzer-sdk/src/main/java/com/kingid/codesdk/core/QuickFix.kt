package com.kingid.codesdk.core

/**
 * إصلاح سريع مقترح لخطأ معين (Quick Fix).
 * على سبيل المثال: إضافة بيان Import تلقائياً، أو إعلان متغير ناقص.
 */
data class QuickFix(
    val id: String,
    val title: String,
    val description: String? = null,
    val diagnosticCode: String,
    val location: DiagnosticLocation,
    val edits: List<TextEdit> = emptyList(),
    val requiresUserConfirmation: Boolean = false
)
