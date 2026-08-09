package com.emm.domain.shared

/**
 * Half-open epoch-millis bounds of one calendar month, `[startInclusive, endExclusive)`.
 *
 * Always built from [YearMonth.range] so the bounds stay timezone-correct: the boundary between two
 * months is a local-time concept, and computing it in SQL from an epoch column would put a
 * transaction recorded late on the 31st into the wrong month for every user off UTC.
 */
data class MonthRange(val startInclusive: Long, val endExclusive: Long)
