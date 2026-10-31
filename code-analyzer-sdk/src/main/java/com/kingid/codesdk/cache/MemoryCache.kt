package com.kingid.codesdk.cache

import com.kingid.codesdk.core.AnalysisResult
import java.util.LinkedHashMap

/**
 * ذاكرة مؤقتة في ذاكرةraal (LRU) محدودة بعدد الإدخالات لمنع تسرب الذاكرة (OOM).
 */
class MemoryCache(private val maxEntries: Int) : AnalysisCache {

    private val cache = LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
        // إزالة الأقدم استخداماً عند تجاوز الحد
        if (size() > maxEntries) removeEldestEntry { it.size() > maxEntries } else false
    }

    override fun get(key: String): CacheEntry? = synchronized(this) { cache[key] }

    override fun put(key: String, entry: CacheEntry) = synchronized(this) { cache[key] = entry }

    override fun remove(key: String) = synchronized(this) { cache.remove(key) }

    override fun clear() = synchronized(this) { cache.clear() }

    override fun size(): Int = synchronized(this) { cache.size }
}
