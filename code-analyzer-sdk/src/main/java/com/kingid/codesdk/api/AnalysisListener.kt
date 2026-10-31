package com.kingid.codesdk.api

import com.kingid.codesdk.core.AnalysisResult
import com.kingid.codesdk.core.Diagnostic
import com.kingid.codesdk.core.QuickFix

/**
 * مستمع لنتائج الفحص والإصلاحات السريعة.
 * تُستدعى جميع طرقه على خيط الواجهة الرئيسية (Main Thread).
 */
interface AnalysisListener {
    fun onAnalysisStarted()
    fun onAnalysisCompleted(result: AnalysisResult)
    fun onFixesReady(diagnostics: List<Diagnostic>, fixes: Map<Diagnostic, List<QuickFix>>)
    fun onError(throwable: Throwable)
}
