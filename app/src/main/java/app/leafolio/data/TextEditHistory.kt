package app.leafolio.data

data class EditorText(val title: String, val content: String)

/** Session-only history. Adjacent typing in one field is undone as one action. */
class TextEditHistory(private val limit: Int = 100) {
    private val past = ArrayDeque<EditorText>()
    private val future = ArrayDeque<EditorText>()
    private var lastField: String? = null
    private var lastTime = Long.MIN_VALUE
    private var lastText: EditorText? = null
    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()

    fun record(before: EditorText, after: EditorText, field: String, now: Long) {
        if (before == after) return
        val continuesTyping = field == lastField && lastText == before && now >= lastTime && now - lastTime <= 750
        if (!continuesTyping) {
            past.addLast(before)
            if (past.size > limit) past.removeFirst()
        }
        future.clear()
        lastField = field
        lastTime = now
        lastText = after
    }

    fun undo(current: EditorText): EditorText {
        endGroup()
        if (!canUndo) return current
        future.addLast(current)
        return past.removeLast()
    }

    fun redo(current: EditorText): EditorText {
        endGroup()
        if (!canRedo) return current
        past.addLast(current)
        return future.removeLast()
    }

    fun endGroup() {
        lastField = null
        lastText = null
    }
}
