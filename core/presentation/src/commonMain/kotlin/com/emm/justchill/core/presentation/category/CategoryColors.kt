package com.emm.justchill.core.presentation.category

val selectableColorIds: List<String> = listOf("blue", "green", "red", "purple", "orange", "gray")

fun colorLabel(colorId: String): String = when (colorId) {
    "blue" -> "Pizarra"
    "green" -> "Salvia"
    "red" -> "Terracota"
    "purple" -> "Malva"
    "orange" -> "Ocre"
    "gray" -> "Grafito"
    else -> colorId
}
