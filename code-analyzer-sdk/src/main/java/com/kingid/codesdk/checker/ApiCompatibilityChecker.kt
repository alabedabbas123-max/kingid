package com.kingid.codesdk.checker

import com.kingid.codesdk.core.*
import com.kingid.codesdk.parser.Document

/**
 * مدقق توافقية مستويات API (API Level Compatibility).
 * يكشف عن استخدام واجهات require مستوى API أعلى من minSdk، والواجهات المهملة (Deprecated APIs).
 */
class ApiCompatibilityChecker(
    private val minApiLevel: Int,
    private val deprecatedApiList: Set<String> = emptySet()
) {

    private val requiresApiRegex = Regex("@RequiresApi\\(\\s*API\\s*levels?\\s*=\\s*(\\d+)\\)", RegexOption.IGNORE_CASE)
    private val apiLevelRegex = Regex("Build\\.VERSION\\.SDK_INT\\s*>=\\s*(\\d+)")

    fun check(document: Document): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val text = document.text

        // @RequiresApi أعلى من minSdk
        for (match in requiresApiRegex.findAll(text)) {
            val required = match.groupValues[1].toIntOrNull() ?: continue
            if (required > minApiLevel) {
                diagnostics += Diagnostic(
                    code = "API_REQUIRES_HIGHER",
                    message = "الواجهة تتطلب API level $required بينما minSdk = $minApiLevel",
                    severity = Severity.WARNING,
                    location = locationAt(document, match),
                    ruleId = "api-compatibility",
                    fixable = false
                )
            }
        }

        // واجهات مهملة مخصصة
        for (deprecated in deprecatedApiList) {
            val index = text.indexOf(deprecated)
            if (index >= 0) {
                diagnostics += Diagnostic(
                    code = "API_DEPRECATED",
                    message = "الواجهة '$deprecated' مهملة وغير موصى باستخدامها",
                    severity = Severity.WARNING,
                    location = DiagnosticLocation(
                        file = document.uri,
                        startLine = -1, startColumn = -1,
                        endLine = -1, endColumn = -1,
                        startOffset = index, endOffset = index + deprecated.length
                    ),
                    ruleId = "api-deprecated",
                    fixable = false,
                    data = mapOf("symbol" to deprecated)
                )
            }
        }
        return diagnostics
    }

    private fun locationAt(document: Document, match: MatchResult): DiagnosticLocation {
        val start = match.range.first
        val end = match.range.last
        return DiagnosticLocation(
            file = document.uri,
            startLine = -1, startColumn = -1,
            endLine = -1, endColumn = -1,
            startOffset = start, endOffset = end
        )
    }
}
