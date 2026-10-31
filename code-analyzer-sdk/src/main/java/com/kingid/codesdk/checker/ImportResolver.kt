package com.kingid.codesdk.checker

import com.kingid.codesdk.core.*
import com.kingid.codesdk.parser.Document

/**
 * حلال الاستيرادات (Import Resolver). يستخرج بيانات import من المصدر ويقارن مع الفهرس المعروف
 * (Known symbols) ليُبلغ عن الاستيرادات المفقودة ويُقترح إضافة import تلقائياً.
 */
class ImportResolver(
    private val knownSymbols: Map<String, String> = emptyMap() // "SimpleName" -> "fully.qualified.Name"
) {

    private val importRegex = Regex("""^\s*import\s+(static\s+)?([\w\.]+)\s*;""", RegexOption.MULTILINE)

    fun resolve(document: Document): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val imported = mutableSetOf<String>()
        for (match in importRegex.findAll(document.text)) {
            imported += match.groupValues[2]
        }

        // استخراج أسماء بسيطة مُستخدمة في النص (تقريبي) ومقارنتها بالمستورد
        val used = extractUsedSimpleNames(document.text)
        for (name in used) {
            if (name in imported) continue
            val fq = knownSymbols[name] ?: continue
            diagnostics += Diagnostic(
                code = "MISSING_IMPORT",
                message = "الاستيراد مفقود للرمز '$name' — يُقترح استيراد $fq",
                severity = Severity.WARNING,
                location = DiagnosticLocation(
                    file = document.uri, startLine = -1, startColumn = -1,
                    endLine = -1, endColumn = -1, startOffset = -1, endOffset = -1
                ),
                ruleId = "missing-import",
                fixable = true,
                data = mapOf("symbol" to name, "suggestedImport" to fq)
            )
        }
        return diagnostics
    }

    private fun extractUsedSimpleNames(text: String): Set<String> {
        val names = mutableSetOf<String>()
        val wordRegex = Regex("\\b[A-Za-z_]\\w*\\b")
        for (match in wordRegex.findAll(text)) {
            names += match.value
        }
        return names
    }
}
