package com.kingid.codesdk.core

/**
 * تعديل نصي واحد يُطبق على ملف. يُستخدم من قبل نظام الإصلاحات السريعة (Quick Fixes).
 */
data class TextEdit(
    val file: String,
    val startOffset: Int,
    val endOffset: Int,
    val replacement: String
) {
    require(startOffset >= 0) { "startOffset يجب أن يكون >= 0" }
    require(endOffset >= startOffset) { "endOffset يجب أن يكون >= startOffset" }
}
