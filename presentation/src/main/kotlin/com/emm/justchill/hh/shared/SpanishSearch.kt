package com.emm.justchill.hh.shared

private val ACCENT_MAP: Map<Char, Char> = mapOf(
    'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n',
    'Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U', 'Ü' to 'U', 'Ñ' to 'N',
)

fun String.stripSpanishAccents(): String = map { ACCENT_MAP[it] ?: it }.joinToString("")
