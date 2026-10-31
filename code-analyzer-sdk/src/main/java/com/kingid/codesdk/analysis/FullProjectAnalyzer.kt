package com.kingid.codesdk.analysis

import com.kingid.codesdk.cache.AnalysisCache
import com.kingid.codesdk.core.AnalysisResult
import com.kingid.codesdk.core.Diagnostic
import com.kingid.codesdk.parser.CodeParser
import com.kingid.codesdk.parser.Document

import java.io.File

/**
 * محرك الفحص الشامل للمشروع (Full Project Analysis).
 * يجول جذور المصادر المحددة، يحلل كل ملف، ويجمع التشخيصات في نتيجة واحدة.
 */
class FullProjectAnalyzer(private val parsers: List<CodeParser>) {

    fun analyze(
        sourceRoots: List<String>,
        cache: AnalysisCache,
        projectId: String
    ): AnalysisResult {
        val allDiagnostics = mutableListOf<Diagnostic>()
        var filesAnalyzed = 0
        val start = System.currentTimeMillis()

        for (root in sourceRoots) {
            for (file in listSourceFiles(root)) {
                val text = readFileSafe(file)
                val parser = chooseParser(file)
                val node = parser.parse(Document(file, text))
                allDiagnostics += diagnose(node, file)
                filesAnalyzed++
            }
        }

        return AnalysisResult(
            sessionId = projectId,
            file = "<project>",
            diagnostics = allDiagnostics.toList(),
            cached = false,
            durationMs = System.currentTimeMillis() - start
        )
    }

    private fun listSourceFiles(root: String): List<String> {
        val supported = parsers.flatMap { it.supportedExtensions }.toMutableSet()
        if (supported.isEmpty()) supported.addAll(setOf("java", "kt"))
        return File(root).walk()
            .filter { it.isFile && supported.contains(it.extension) }
            .map { it.absolutePath }
            .toList()
    }

    private fun chooseParser(file: String): CodeParser {
        return parsers.firstOrNull { it.supportedExtensions.any { file.endsWith(it) } }
            ?: parsers.firstOrNull()
            ?: throw IllegalStateException("لا يوجد محلل أكواد مسجل في المحرك")
    }

    private fun diagnose(node: com.kingid.codesdk.parser.AstNode, file: String): List<Diagnostic> {
        // تُنفذ المدققات هنا على الشجرة المستخلصة (AST).
        return emptyList()
    }

    private fun readFileSafe(path: String): String {
        return try {
            java.nio.file.Files.readString(java.nio.file.Paths.get(path))
        } catch (e: Exception) {
            ""
        }
    }
}
