package com.kingid.codesdk.api

import com.kingid.codesdk.core.ChangeRange
import com.kingid.codesdk.core.QuickFix

/**
 * تعميم لواجهة المحرر (Editor) الخاصة بالتطبيق المضيف.
 * يتيح للمكتبة الالتصاق بأي محرر نصوص (EditText، View مخصص، WebView، إلخ) دون ربط بالمكتبة المضيفة.
 */
interface EditorAttachable {
    /** نص المحرر الحالي. */
    val text: String

    /** تسجيل مستمع للتغييرات التي تطرأ على النص. */
    fun setOnTextChangeListener(listener: (text: String, range: ChangeRange) -> Unit)

    /** تطبيق إصلاح سريع على المحرر عبر التطبيق المضيف. */
    fun applyFix(fix: QuickFix, callback: (Boolean) -> Unit)
}
