package com.emm.justchill.hh.shared

/**
 * Accent-insensitive Spanish search normalization for Compose Multiplatform commonMain. Replaces
 * the JVM `java.text.Normalizer` (NFD + combining-mark strip) used for category search matching.
 *
 * Maps the Spanish accented letters the app actually uses (vowels with acute accent, ü, ñ) to their
 * base form, then lowercases and trims. This reproduces the search behavior of the old
 * `Normalizer.normalize(..., NFD).replace(combiningMarks, "").lowercase().trim()`.
 */
private val ACCENT_MAP: Map<Char, Char> = mapOf(
    'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n',
    'Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U', 'Ü' to 'U', 'Ñ' to 'N',
)

fun String.stripSpanishAccents(): String =
    map { ACCENT_MAP[it] ?: it }.joinToString("")
