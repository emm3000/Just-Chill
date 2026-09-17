package com.emm.justchill.core.ui.atoms

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The early return preserves IME composition state; a rebuilt [TextFieldValue] would drop it.
 * A refused keystroke otherwise leaves the caret put instead of walking it over a character
 * that was never inserted.
 */
internal fun TextFieldValue.rewrittenTo(filtered: String): TextFieldValue {
    if (text == filtered) return this
    val start = cursorAfterFiltering(text, selection.start, filtered)
    val end = cursorAfterFiltering(text, selection.end, filtered)
    return TextFieldValue(text = filtered, selection = TextRange(start, end))
}

/**
 * [filtered] must be [typed] with characters only deleted; this greedily matches it against
 * [typed] to recover exactly what survived, caret included.
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
