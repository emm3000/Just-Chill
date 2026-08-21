package com.emm.domain.loan

private val WHITESPACE_RUN = Regex("\\s+")

private val ACCENT_FOLD_MAP: Map<Char, Char> = mapOf(
    'á' to 'a', 'à' to 'a', 'â' to 'a', 'ä' to 'a', 'ã' to 'a',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
    'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'ö' to 'o', 'õ' to 'o',
    'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
    // `ñ` folds to `n` deliberately: "Muñoz" typed as "Munoz" is the exact split this feature exists
    // to prevent. The mirror cost — two genuinely different people merging into one row — is already
    // accepted in docs/work/epics/E05-loans.md.
    'ñ' to 'n',
    'ç' to 'c',
)

internal fun personKey(personName: String): String {
    val lowercased = personName.lowercase()
    val folded = buildString {
        for (char in lowercased) append(ACCENT_FOLD_MAP[char] ?: char)
    }
    return folded.replace(WHITESPACE_RUN, " ").trim()
}
