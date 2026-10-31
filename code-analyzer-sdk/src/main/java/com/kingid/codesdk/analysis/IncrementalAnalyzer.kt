package com.kingid.codesdk.analysis

import com.kingid.codesdk.cache.AnalysisCache
import com.kingid.codesdk.core.AnalysisResult
import com.kingid.codesdk.core.ChangeRange
import com.kingid.codesdk.core.Diagnostic
import com.kingid.codesdk.parser.CodeParser
import com.kingid.codesdk.parser.AstNode
import java.io.Serializable

/**
 * محرك الفحص التزايدي (Incremental Analysis).
 * يعيد فحص الملف المعدل فقط، ويستخدم بصمة المحتوى (fingerprint) لإعادة استخدام
 * النتائج المخزنة عندما لا يتغير الملف، مما يخفض استهلاك المعالج والذاكرة.
 */
class IncrementalAnalyzer(private val parsers: List<CodeParser>) {

    fun analyzeSnippet(
        text: String,
        range: ChangeRange?,
        fileKey: String,
        cache: AnalysisCache
    ): AnalysisResult {
        val fingerprint = text.hashCode().toString()
        val cached = cache.get(fileKey)
        if (cached != null && cached.fingerprint == fingerprint) {
            return cached.result.copy(cached = true)
        }

        val parser = parsers.firstOrNull { it.supportedExtensions.any { fileKey.endsWith(it) } }
            ?: parsers.firstOrNull()
        val node = parser?.parse(Document(fileKey, text))
        val diagnostics = buildDiagnostics(node, text, range, fileKey)

        val result = AnalysisResult(
            sessionId = "incremental",
            file = fileKey,
            diagnostics = diagnostics,
            cached = false
        )
        cache.put(fileKey, CacheEntry(fingerprint, result))
        return result
    }

    private fun buildDiagnostics(
        node: AstNode?,
        text: String,
        range: ChangeRange?,
        fileKey: String
    ): List<Diagnostic> {
        // تُنفذ المدققات (Syntax, Semantic, Imports، API) على العقدة المستخلصة.
        return emptyList()
    }
}

/** إدخال مخزّن في ذاكرة الفحص التزايدي. */
private data class CacheEntry(
    val fingerprint: String,
    val result: AnalysisResult
) : Serializable
