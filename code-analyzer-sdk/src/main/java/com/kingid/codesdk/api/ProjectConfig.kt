package com.kingid.codesdk.api

import android.content.Context
import com.kingid.codesdk.core.AnalysisListener

/**
 * إعدادات جلسة الفحص. يُمرر كائن التكوين إلى createSession() لتحديد سلوك المحرك.
 */
data class ProjectConfig(
    val context: Context,
    val projectId: String = "default",
    val sourceRoots: List<String> = emptyList(),
    val javaEnabled: Boolean = true,
    val kotlinEnabled: Boolean = true,
    val minApiLevel: Int = 21,
    val targetApiLevel: Int = 34,
    val checkDeprecatedApis: Boolean = true,
    val deprecatedApiList: Set<String> = emptySet(),
    val debounceMs: Long = 300,
    val maxCacheEntries: Int = 500,
    val listener: AnalysisListener? = null
) {
    companion object {
        fun default(context: Context): ProjectConfig = ProjectConfig(context = context)
    }
}
