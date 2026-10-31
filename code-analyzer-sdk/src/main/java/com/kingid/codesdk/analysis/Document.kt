package com.kingid.codesdk.analysis

/**
 * مستند نصي يتم تحليله، يحمل معرف(file URI) والمحتوى ولغة البرمجة.
 */
data class Document(
    val uri: String,
    val text: String,
    val version: Long = 0,
    val language: String = "java"
)
