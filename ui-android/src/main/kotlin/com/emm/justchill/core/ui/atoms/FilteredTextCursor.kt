package com.emm.justchill.core.ui.atoms

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * This edit rewritten to [filtered], with the caret left after the same surviving characters it
 * stood after — a refused keystroke leaves it put instead of walking it over a character that was
 * never inserted. An unchanged rewrite hands the value straight back, IME composition and all.
 */
internal fun TextFieldValue.rewrittenTo(filtered: String): TextFieldValue {
    if (text == filtered) return this
    val start = cursorAfterFiltering(text, selection.start, filtered)
    val end = cursorAfterFiltering(text, selection.end, filtered)
    return TextFieldValue(text = filtered, selection = TextRange(start, end))
}

/**
 * How many of the first [cursor] characters of [typed] survive into [filtered] — the caret's
 * offset once the rewrite lands. [filtered] must be [typed] with characters only deleted, so
 * greedily matching it against [typed] recovers exactly what survived, caret included.
 */
internal fun cursorAfterFiltering(typed: String, cursor: Int, filtered: String): Int {
    var kept = 0
    var mapped = 0
    for (index in typed.indices) {
        if (index == cursor) mapped = kept
        if (kept < filtered.length && typed[index] == filtered[kept]) kept++
    }
    if (cursor >= typed.length) mapped = kept
    return if (kept == filtered.length) mapped else filtered.length
}
