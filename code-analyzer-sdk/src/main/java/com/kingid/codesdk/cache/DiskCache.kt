package com.kingid.codesdk.cache

import com.kingid.codesdk.core.AnalysisResult
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * ذاكرة مؤقتة على القرص تُغلف ذاكرة الذاكرة (MemoryCache) وتستعيد النتائج عند إعادة تشغيل العملية.
 * تُسلسل نتائج الفحص ككائنات في مجلد cache الخاص بالتطبيق.
 */
class DiskCache(
    private val dir: File,
    private val memory: MemoryCache
) : AnalysisCache {

    init { dir.mkdirs() }

    private fun fileFor(key: String): File = File(dir, "analysis_${key.hashCode()}.cache")

    override fun get(key: String): CacheEntry? {
        memory.get(key)?.let { return it }
        val file = fileFor(key)
        return if (file.exists()) {
            try {
                ObjectInputStream(java.io.FileInputStream(file)).use { it.readObject() as CacheEntry }
            } catch (e: Exception) {
                null
            }
        } else null
    }

    override fun put(key: String, entry: CacheEntry) {
        memory.put(key, entry)
        val file = fileFor(key)
        try {
            ObjectOutputStream(java.io.FileOutputStream(file)).use { it.writeObject(entry) }
        } catch (e: Exception) {
            // تجاهل أخطاء الكتابة على القرص — الذاكرة الأساسية ما زالت متاحة
        }
    }

    override fun remove(key: String) {
        memory.remove(key)
        fileFor(key).delete()
    }

    override fun clear() {
        memory.clear()
        dir.listFiles()?.forEach { it.delete() }
    }

    override fun size(): Int = memory.size()
}
