package com.kingid.codesdk.cache

import android.content.Context
import com.kingid.codesdk.core.AnalysisResult

/**
 * واجهة ذاكرة التخزين المؤقت لنتائج الفحص. تدعم 저장اً في الذاكرة (RAM) وقراءة/كتابة على القرص.
 */
interface AnalysisCache {
    fun get(key: String): CacheEntry?
    fun put(key: String, entry: CacheEntry)
    fun remove(key: String)
    fun clear()
    fun size(): Int
}

/** إدخال في ذاكرة التخزين المؤقت. */
data class CacheEntry(
    val fingerprint: String,
    val result: AnalysisResult,
    val cachedAt: Long = System.currentTimeMillis()
)

/** إنشاء نسخة من ذاكرة التخزين المؤقت (ذاكرة محدودة + قرص اختياري). */
fun create(cacheDir: java.io.File?, maxEntries: Int): AnalysisCache {
    val memory = MemoryCache(maxEntries)
    return if (cacheDir != null && cacheDir.exists()) {
        DiskCache(cacheDir, memory)
    } else {
        memory
    }
}
