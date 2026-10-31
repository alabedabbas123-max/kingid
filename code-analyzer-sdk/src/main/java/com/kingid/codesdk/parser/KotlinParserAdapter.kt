package com.kingid.codesdk.parser

/**
 * محول لمترجم Kotlin (org.jetbrains.kotlin.compiler) — يُختار عند الحاجة لتحليل Kotlin كامل الترميز.
 * ملاحظة: ليست جزءاً من الـ AAR الأساسي. يُفعّل同上JavaParserAdapter عند الحاجة.
 */
class KotlinParserAdapter : CodeParser {

    override val supportedExtensions: Set<String> = setOf(".kt", ".kts")

    override fun parse(document: Document): AstNode {
        return AstNode(
            type = "KtFile",
            text = document.text,
            startOffset = 0,
            endOffset = document.text.length,
            meta = mapOf("parser" to "KotlinParserAdapter-stub")
        )
    }
}
