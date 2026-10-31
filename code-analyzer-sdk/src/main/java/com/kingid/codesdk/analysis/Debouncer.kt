package com.kingid.codesdk.analysis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * آلية الت veio (Debounce) عالية الأداء لتجنب الفحص المفرط عند الكتابة السريعة.
 * تحتفظ بآخر تغيير فقط، وتقوم بإرجاعه عبر استدعاءgada (callback) بعد انقضاء المهلة
 * دون أي تغيير جديد. 모든 العمليات تُنفذ على Dispatchers.Default.
 */
class Debouncer(var delayMs: Long = 300) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    @Volatile private var pending: PendingChange? = null
    private var job: kotlinx.coroutines.Job? = null

    /**
     * إرسال تغيير جديد للمعالجة.
     * إذا تم إرسال تغيير جديد خلال المهلة، تُلغى المهمة السابقة وتبدأ مهلة جديدة من الصفر.
     */
    fun submit(
        text: String,
        range: ChangeRange?,
        fileKey: String,
        onTimeout: (PendingChange) -> Unit
    ) {
        synchronized(lock) {
            pending = PendingChange(text, range, fileKey)
            job?.cancel()
        }
        job = scope.launch {
            delay(delayMs)
            val change = synchronized(lock) {
                val c = pending
                pending = null
                c
            }
            if (change != null) {
                // نشر المعالجة على خيط الواجهة الرئيسية ليتولى المستعجل تشغيل التحليل في خلفيته
                withContext(Dispatchers.Main) {
                    onTimeout(change)
                }
            }
        }
    }

    /** إلغاء المهمة المؤجلة ومسح التغيير المعلق. */
    fun cancel() {
        synchronized(lock) {
            pending = null
            job?.cancel()
            job = null
        }
    }
}

/** تغيير معلق في انتظار المعالجة بعد انتهاء مهلة الت veio. */
data class PendingChange(
    val text: String,
    val range: ChangeRange?,
    val fileKey: String
)
