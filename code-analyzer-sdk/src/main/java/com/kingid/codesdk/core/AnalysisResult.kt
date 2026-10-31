package com.kingid.codesdk.core

import java.io.Serializable

/**
 * نتيجة تشغيل دورة فحص على ملف واحد أو على المشروع بالكامل.
 */
data class AnalysisResult(
    val sessionId: String,
    val file: String,
    val diagnostics: List<Diagnostic>,
    val timestampMs: Long = System.currentTimeMillis(),
    val cached: Boolean = false,
    val durationMs: Long = 0
) {
    val errorsCount: Int get() = diagnostics.count { it.isError() }
    val warningsCount: Int get() = diagnostics.count { it.isWarning() }
    val hasErrors: Boolean get() = errorsCount > 0
} : Serializable
