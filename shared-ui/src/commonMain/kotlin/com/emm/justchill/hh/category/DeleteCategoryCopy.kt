package com.emm.justchill.hh.category

/**
 * Body copy for the delete-category confirmation.
 *
 * The dialog used to say "Los movimientos asociados pasarán a «Sin categoría»" with no number
 * attached, so deleting a category the user had never used read exactly like deleting the one
 * holding four years of history. The count is the whole point of the warning.
 */
internal fun buildDeleteCategoryMessage(affectedCount: Int): String = when (affectedCount) {
    0 -> "Ningún movimiento la usa, así que no cambia nada de tu historial."
    1 -> "1 movimiento va a quedar sin categoría. No podés deshacerlo desde la app."
    else -> "$affectedCount movimientos van a quedar sin categoría. No podés deshacerlo desde la app."
}
