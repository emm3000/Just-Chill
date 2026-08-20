package com.emm.justchill.hh.category

fun buildDeleteCategoryMessage(affectedCount: Int): String = when (affectedCount) {
    0 -> "Ningún movimiento la usa, así que no cambia nada de tu historial."
    1 -> "1 movimiento va a quedar sin categoría. No puedes deshacerlo desde la app."
    else -> "$affectedCount movimientos van a quedar sin categoría. No puedes deshacerlo desde la app."
}
