package com.emm.data.shared

import kotlin.enums.enumEntries

/**
 * Returns the enum entry whose [Enum.name] exactly matches [value] (case-sensitive), or null if no
 * entry matches.
 *
 * Used in data→domain mappers wherever a column value originates from a remote device. An unknown
 * value means the row was written by a newer app version or contains schema drift; the correct
 * response is to skip the row and let it resurface once the local app version recognises the value.
 * Silently coercing to a default (e.g. treating "INCOME" as Income) would corrupt financial totals
 * and is never acceptable.
 *
 * **Uniformly absent policy**: an unparseable row behaves as absent everywhere — skipped in lists
 * (via `mapNotNull`) AND null from single-row `find()` calls (via `...OrNull` mappers). A row the
 * app cannot interpret is invisible in all views rather than partially visible (e.g. shown without
 * a type, or misclassified in an aggregate).
 */
inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? = enumEntries<T>().firstOrNull { it.name == value }
