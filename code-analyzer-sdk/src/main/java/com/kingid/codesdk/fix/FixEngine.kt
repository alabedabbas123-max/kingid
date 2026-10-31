package com.kingid.codesdk.fix

import com.kingid.codesdk.core.*
import com.kingid.codesdk.checker.ImportResolver

/**
 * محرك الإصلاحات السريعة (Fix Engine).
 * يحوّل كل تشخيص إلى قائمة من الإصلاحات المقترحة (QuickFixes) يمكن للتطبيق المضيف
 * استعراضها وتطبيقها عبر المحرر الملحق.
 */
class FixEngine {

    fun proposeFixes(diagnostic: Diagnostic): List<QuickFix> {
        return when (diagnostic.ruleId) {
            "missing-import" -> addImportFixes(diagnostic)
            "undeclared-variable" -> undeclaredVariableFixes(diagnostic)
            else -> emptyList()
        }
    }

    private fun addImportFixes(diagnostic: Diagnostic): List<QuickFix> {
        val symbol = diagnostic.data["symbol"] as? String ?: return emptyList()
        val suggestedImport = diagnostic.data["suggestedImport"] as? String ?: return emptyList()
        return listOf(
            QuickFix(
                id = "fix-add-import-$symbol",
                title = "أضف استيراد: $suggestedImport",
                description = "يضيف بيان import تلقائياً في أعلى الملف لحل الخطأ '${diagnostic.code}'",
                diagnosticCode = diagnostic.code,
                location = diagnostic.location,
                edits = listOf(
                    TextEdit(
                        file = diagnostic.location.file,
                        startOffset = 0,
                        endOffset = 0,
                        replacement = "import $suggestedImport;\n"
                    )
                ),
                requiresUserConfirmation = false
            )
        )
    }

    private fun undeclaredVariableFixes(diagnostic: Diagnostic): List<QuickFix> {
        val symbol = diagnostic.data["symbol"] as? String ?: return emptyList()
        return listOf(
            QuickFix(
                id = "fix-declare-$symbol",
                title = "أعلِن عن المتغير: $symbol",
                description = "يقترح إعلاناً أولياً للمتغير غير المعلن '${diagnostic.code}'",
                diagnosticCode = diagnostic.code,
                location = diagnostic.location,
                edits = listOf(
                    TextEdit(
                        file = diagnostic.location.file,
                        startOffset = diagnostic.location.startOffset.coerceAtLeast(0),
                        endOffset = diagnostic.location.endOffset.coerceAtLeast(0),
                        replacement = "val $symbol = null\n"
                    )
                ),
                requiresUserConfirmation = true
            )
        )
    }
}
