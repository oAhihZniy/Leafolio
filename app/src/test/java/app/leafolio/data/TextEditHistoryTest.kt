package app.leafolio.data

import org.junit.Assert.*
import org.junit.Test

class TextEditHistoryTest {
    private val blank = EditorText("", "")

    @Test fun typingGroupsButPausesCreateSeparateUndoSteps() {
        val history = TextEditHistory()
        val first = blank.copy(content = "你")
        val second = blank.copy(content = "你好")
        val third = blank.copy(content = "你好世界")
        history.record(blank, first, "content", 0)
        history.record(first, second, "content", 200)
        history.record(second, third, "content", 1500)
        assertEquals(second, history.undo(third))
        assertEquals(blank, history.undo(second))
        assertFalse(history.canUndo)
        assertEquals(second, history.redo(blank))
        assertEquals(third, history.redo(second))
        assertFalse(history.canRedo)
    }

    @Test fun changingFieldsPreservesBothTitleAndBody() {
        val history = TextEditHistory()
        val titled = blank.copy(title = "旅行")
        val body = titled.copy(content = "海边")
        history.record(blank, titled, "title", 0)
        history.record(titled, body, "content", 100)
        assertEquals(titled, history.undo(body))
        assertEquals(blank, history.undo(titled))
    }

    @Test fun editingAfterUndoDiscardsRedoAndStartsNewGroup() {
        val history = TextEditHistory()
        val first = blank.copy(content = "第一版")
        val replacement = blank.copy(content = "改写")
        history.record(blank, first, "content", 0)
        assertEquals(blank, history.undo(first))
        history.record(blank, replacement, "content", 100)
        assertFalse(history.canRedo)
        assertEquals(blank, history.undo(replacement))
        assertEquals(replacement, history.redo(blank))
    }

    @Test fun unchangedTextDoesNotAddStepsOrDiscardRedo() {
        val history = TextEditHistory()
        val text = blank.copy(content = "粘贴一整段\n第二行")
        history.record(blank, text, "content", 0)
        history.undo(text)
        history.record(blank, blank, "content", 100)
        assertTrue(history.canRedo)
        assertFalse(history.canUndo)
    }

    @Test fun historyIsBoundedAndEmptyActionsAreSafe() {
        val history = TextEditHistory(limit = 2)
        val one = blank.copy(content = "1")
        val two = blank.copy(content = "2")
        val three = blank.copy(content = "3")
        assertEquals(blank, history.undo(blank))
        assertEquals(blank, history.redo(blank))
        history.record(blank, one, "content", 0)
        history.record(one, two, "content", 1000)
        history.record(two, three, "content", 2000)
        assertEquals(two, history.undo(three))
        assertEquals(one, history.undo(two))
        assertFalse(history.canUndo)
    }
}
