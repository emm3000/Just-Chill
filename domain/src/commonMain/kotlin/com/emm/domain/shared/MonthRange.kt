package com.emm.domain.shared

/**
 * Half-open bounds of one calendar month, `[startInclusive, endExclusive)`, as ISO day strings
 * (`'2026-08-01'`).
 *
 * Always built from [YearMonth.range]. The bounds need no timezone because the column they filter
 * needs none: `transactions.occurredAt` is ISO local text, so `>= '2026-08-01' AND < '2026-09-01'`
 * is an ordinary string comparison that picks out exactly the days of that month — including the
 * last instant of the 31st, because `'T'` sorts after every digit.
 */
data class MonthRange(val startInclusive: String, val endExclusive: String)
