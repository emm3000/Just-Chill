package com.emm.data.shared

import kotlin.enums.enumEntries

/**
 * An unrecognised value skips the row; never coerce to a default — treating Income as Spend
 * corrupts financial totals. Absence is uniform: such a row drops out of lists and comes back
 * null from find(), never partially visible.
 */
inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? = enumEntries<T>().firstOrNull { it.name == value }
