package com.kingid.codesdk.core

/**
 * نطاق تغيير حدث في مستند المحرر (سطور وأوفستات)، يُستخدم في الفحص التزايدي.
 */
data class ChangeRange(
    val startLine: Int,
    val endLine: Int,
    val startOffset: Int,
    val endOffset: Int
)
