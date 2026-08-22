package com.emm.justchill.core.ui.atoms

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Every `filtered` below is what `sanitizeInterestPercentInput` (`:presentation`) makes of the
 * `typed` beside it — that module's own suite pins those pairs; this one pins only the caret.
 */
class FilteredTextCursorTest {

    @Test
    fun `an empty field keeps the caret at zero`() {
        assertEquals(0, cursorAfterFiltering(typed = "", cursor = 0, filtered = ""))
    }

    @Test
    fun `a caret at the start stays at the start`() {
        assertEquals(0, cursorAfterFiltering(typed = "a12.50", cursor = 1, filtered = "12.50"))
    }

    @Test
    fun `a caret at the end lands at the end of the shorter text`() {
        assertEquals(5, cursorAfterFiltering(typed = "12.349", cursor = 6, filtered = "12.34"))
    }

    @Test
    fun `a refused character mid string leaves the caret put`() {
        assertEquals(2, cursorAfterFiltering(typed = "12a.34", cursor = 3, filtered = "12.34"))
    }

    @Test
    fun `a backspace mid string keeps the caret where the character was`() {
        assertEquals(1, cursorAfterFiltering(typed = "1.50", cursor = 1, filtered = "1.50"))
    }

    @Test
    fun `a paste that is partly refused leaves the caret after what survived`() {
        assertEquals(2, cursorAfterFiltering(typed = "1abc9.50", cursor = 5, filtered = "19.50"))
    }

    @Test
    fun `a paste refused outright leaves the caret where it started`() {
        assertEquals(1, cursorAfterFiltering(typed = "1abc.50", cursor = 4, filtered = "1.50"))
    }

    @Test
    fun `a second separator hands the caret to the one that survived`() {
        assertEquals(3, cursorAfterFiltering(typed = "12..34", cursor = 3, filtered = "12.34"))
    }

    @Test
    fun `every character refused puts the caret at zero`() {
        assertEquals(0, cursorAfterFiltering(typed = "abc", cursor = 3, filtered = ""))
    }

    @Test
    fun `text that did not come from this edit sends the caret to its end`() {
        assertEquals(1, cursorAfterFiltering(typed = "12.5", cursor = 2, filtered = "8"))
    }

    @Test
    fun `an unchanged rewrite hands back the very same value`() {
        val typed = TextFieldValue("12.5", TextRange(2))
        assertSame(typed, typed.rewrittenTo("12.5"))
    }

    @Test
    fun `a rewrite renders the text it was handed`() {
        val result = TextFieldValue("12a.34", TextRange(3)).rewrittenTo("12.34")
        assertEquals("12.34", result.text)
        assertEquals(TextRange(2), result.selection)
    }

    @Test
    fun `a rewrite maps both ends of a selection`() {
        val result = TextFieldValue("1abc9.50", TextRange(1, 5)).rewrittenTo("19.50")
        assertEquals(TextRange(1, 2), result.selection)
    }
}
