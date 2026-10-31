package com.kingid.codesdk.parser

/**
 * عقدة في شجرة التركيب المجردة المبسطة (AST) التي ينتجها المحلل.
 */
data class AstNode(
    val type: String,
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val children: List<AstNode> = emptyList(),
    val meta: Map<String, Any> = emptyMap()
) {
    fun findAll(predicate: (AstNode) -> Boolean): List<AstNode> {
        val result = mutableListOf<AstNode>()
        fun visit(node: AstNode) {
            if (predicate(node)) result += node
            node.children.forEach { visit(it) }
        }
        visit(this)
        return result
    }
}
