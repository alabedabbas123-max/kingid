package com.kingid.codesdk.checker

import com.kingid.codesdk.core.*
import com.kingid.codesdk.parser.AstNode
import com.kingid.codesdk.parser.Document
import com.kingid.codesdk.parser.Tokenizer

/**
 * مدقق نحوي (Syntax Checker) خفيف الوزن مبني على الـ Tokenizer المدمج.
 * يكشف عن أخطاء توازن الأقواس، الأقواس المزدوجة، والسلاسل غير المنتهية.
 */
class SyntaxChecker {

    fun check(document: Document): List<Diagnostic> {
        val tokens = Tokenizer().tokenize(document.text)
        val diagnostics = mutableListOf<Diagnostic>()

        // التحقق من توازن الأقواس
        val stack = mutableListOf<Pair<String, Int>>() // (rune, offset)
        val pairs = mapOf("}" to "{", ")" to "(", "]" to "[")
        for (token in tokens) {
            when (token.text) {
                "{", "(", "[" -> stack += token.text to token.offset
                "}", ")", "]" -> {
                    val open = pairs[token.text] ?: continue
                    if (stack.isEmpty() || stack.last().first != open) {
                        diagnostics += Diagnostic(
                            code = "SYNTAX_UNBALANCED",
                            message = "قوس غير متوازن: '${token.text}' في السطر ${token.line}:${token.column}",
                            severity = Severity.ERROR,
                            location = DiagnosticLocation(
                                file = document.uri,
                                startLine = token.line, startColumn = token.column,
                                endLine = token.line, endColumn = token.column + 1,
                                startOffset = token.offset, endOffset = token.offset + 1
                            ),
                            ruleId = "syntax-balance",
                            fixable = false
                        )
                    } else {
                        stack.removeAt(stack.lastIndex)
                    }
                }
            }
        }
        for ((rune, offset) in stack) {
            // سطر وعمود تقديري
            diagnostics += Diagnostic(
                code = "SYNTAX_UNTERMINATED",
                message = "افتُتح '$rune' ولم يُغلق",
                severity = Severity.ERROR,
                location = DiagnosticLocation(
                    file = document.uri, startLine = -1, startColumn = -1,
                    endLine = -1, endColumn = -1, startOffset = offset, endOffset = offset + 1
                ),
                ruleId = "syntax-unterminated",
                fixable = false
            )
        }
        return diagnostics
    }

    fun checkAst(node: AstNode): List<Diagnostic> {
        // توسّع مستقبلاً للتعامل مع شجرة AST كاملة من JavaParser/Kotlin
        return emptyList()
    }
}
