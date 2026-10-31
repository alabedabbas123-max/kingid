package com.kingid.codesdk.parser

/**
 * محول لمكتبة JavaParser (com.github.javaparser) — يُختار عند الحاجة لتحليل Java كامل الترميز (AST حقيقي).
 * ملاحظة: هذه المكتبة ليست جزءاً من الـ AAR الأساسي للحفاظ على خفة الوزن.
 * لتفعيلها، أضفdependencia في التطبيق المضيف ثم نفّذ واجهة CodeParser هنا.
 */
class JavaParserAdapter : CodeParser {

    override val supportedExtensions: Set<String> = setOf(".java")

    override fun parse(document: Document): AstNode {
        // TODO:Wire مع JavaParser عند توفر المكتبة:
        // val parser = JavaParser()
        // val result = parser.parse(text)
        // return toAstNode(result.getResult().orElseThrow())
        return AstNode(
            type = "CompilationUnit",
            text = document.text,
            startOffset = 0,
            endOffset = document.text.length,
            meta = mapOf("parser" to "JavaParserAdapter-stub")
        )
    }
}
