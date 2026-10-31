package com.kingid.codesdk.checker

import com.kingid.codesdk.core.*
import com.kingid.codesdk.parser.Document

/**
 * مدقق دلالي مبسط (Semantic Checker).
 * يُحافظ على جدول رموز بسيط للكيانات المعلنة (classes، functions، variables) ويُبلغ عن
 * المتغيرات المستخدمة بدون إعلان (تقريبي).
 */
class SemanticChecker {

    fun check(document: Document): List<Diagnostic> {
        val text = document.text
        val diagnostics = mutableListOf<Diagnostic>()

        val declared = mutableSetOf<String>()
        val declRegex = Regex("""\b(?:class|interface|fun|val|var)\s+([A-Za-z_]\w*)""")
        for (match in declRegex.findAll(text)) {
            declared += match.groupValues[1]
        }

        val used = Regex("""\b([A-Za-z_]\w*)\b""").findAll(text).map { it.groupValues[1] }.toSet()
        for (name in used) {
            if (name in declared) continue
            when (name) {
                "if", "else", "for", "while", "do", "return", "class", "interface", "fun",
                "val", "var", "object", "sealed", "data", "enum", "when", "try", "catch",
                "finally", "throw", "null", "true", "false", "this", "super", "continue",
                "break", "in", "is", "as", "import", "package", "by", "where" -> continue
            }
            val index = text.indexOf(name)
            if (index >= 0) {
                diagnostics += Diagnostic(
                    code = "UNDECLARED_VARIABLE",
                    message = "الرمز '$name' مستخدم دون أن يُعلن",
                    severity = Severity.ERROR,
                    location = DiagnosticLocation(
                        file = document.uri,
                        startLine = -1, startColumn = -1,
                        endLine = -1, endColumn = -1,
                        startOffset = index, endOffset = index + name.length
                    ),
                    ruleId = "undeclared-variable",
                    fixable = true,
                    data = mapOf("symbol" to name)
                )
            }
        }
        return diagnostics
    }
}
