package com.kingid.codesdk.editor

import com.kingid.codesdk.api.EditorAttachable
import com.kingid.codesdk.core.ChangeRange
import com.kingid.codesdk.core.QuickFix

/**
 * محول (Adapter) يربط محرر المضيف بأي واجهة نصوص تُنفذ EditorAttachable.
 * يمكن للتطبيق المضيف تقديم تنفيذه الخاص أو استخدام هذا المحول مع أي View يعرض نصوصاً.
 */
class HostEditorAdapter(private val delegate: EditorAttachable) : EditorAttachable by delegate
