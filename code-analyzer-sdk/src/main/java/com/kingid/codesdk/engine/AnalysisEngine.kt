package com.kingid.codesdk.engine

import android.content.Context
import com.kingid.codesdk.analysis.*
import com.kingid.codesdk.cache.AnalysisCache
import com.kingid.codesdk.checker.*
import com.kingid.codesdk.core.*
import com.kingid.codesdk.parser.CodeParser
import com.kingid.codesdk.parser.Document

/**
 * المحرك المركزي للتحليل (Analysis Engine).
 * يربط بين المحللين (Parsers)، المدققات (Checkers)، محلييّ التحليل (Full/Incremental)،
 * وذاكرة التخزين المؤقت (Cache).
 */
class AnalysisEngine(private val config: com.kingid.codesdk.api.ProjectConfig) {

    val context: Context = config.context.applicationContext

    // المحلّلون اللغويون (يُسجل محلّل افتراضي خفيف الوزن؛ يمكن إضافة JavaParserAdapter/KotlinParserAdapter)
    val parsers: List<CodeParser> = listOf(
        com.kingid.codesdk.parser.JavaParserAdapter(),
        com.kingid.codesdk.parser.KotlinParserAdapter()
    )

    // المدققات
    val syntaxChecker = SyntaxChecker()
    val semanticChecker = SemanticChecker()
    val importResolver = ImportResolver()
    val apiChecker = ApiCompatibilityChecker(config.minApiLevel, config.deprecatedApiList)

    // محلييّ التحليل
    val fullAnalyzer = FullProjectAnalyzer(parsers)
    val incrementalAnalyzer = IncrementalAnalyzer(parsers)

    /** آلية الت veio (Debounce) المشتركة للجلسات. */
    val debouncer = Debouncer(config.debounceMs)

    /** تحليل ملف واحد. */
    fun analyzeFile(filePath: String, cache: AnalysisCache): AnalysisResult {
        val text = readFileSafe(filePath)
        val doc = Document(filePath, text)
        val diagnostics = runCheckers(doc)
        return AnalysisResult(
            sessionId = config.projectId,
            file = filePath,
            diagnostics = diagnostics,
            cached = false
        )
    }

    /** تحليل كامل المشروع. */
    fun analyzeProject(sourceRoots: List<String>, cache: AnalysisCache, projectId: String): AnalysisResult {
        return fullAnalyzer.analyze(sourceRoots, cache, projectId)
    }

    /** تحليل مقتطف نصي (تزايدي). */
    fun analyzeSnippet(
        text: String,
        range: ChangeRange?,
        fileKey: String,
        cache: AnalysisCache
    ): AnalysisResult {
        val doc = Document(fileKey, text)
        val diagnostics = runCheckers(doc)
        return AnalysisResult(
            sessionId = config.projectId,
            file = fileKey,
            diagnostics = diagnostics,
            cached = false
        )
    }

    private fun runCheckers(doc: Document): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        diagnostics += syntaxChecker.check(doc)
        diagnostics += semanticChecker.check(doc)
        diagnostics += importResolver.resolve(doc)
        diagnostics += apiChecker.check(doc)
        return diagnostics
    }

    private fun readFileSafe(path: String): String {
        return try {
            java.nio.file.Files.readString(java.nio.file.Paths.get(path))
        } catch (e: Exception) {
            ""
        }
    }
}
