package com.kingid.codesdk.parser

/**
 * واجهة محلل الأكواد (Code Parser). كل لغة تُنفذ محلاًً خاصاً يحول النص المصدر إلى شجرة AST مبسطة.
 */
interface CodeParser {
    /** امتدادات الملفات التي يدعمها هذا المحلل (مثلاً ".java" أو ".kt"). */
    val supportedExtensions: Set<String>

    /** تحليل مستند إلى شجرة عقد (AstNode). */
    fun parse(document: Document): AstNode
}

/** مستند نصي يُمرر إلى المحلل. */
data class Document(
    val uri: String,
    val text: String,
    val version: Long = 0,
    val language: String = "java"
)
