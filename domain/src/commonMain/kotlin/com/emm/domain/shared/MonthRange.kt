package com.emm.domain.shared

/**
 * Half-open bounds of one calendar month, `[startInclusive, endExclusive)`, as ISO day strings
 * (`'2026-08-01'`).
 *
 * Always built from [YearMonth.range]. The bounds need no timezone because the column they filter
 * needs none: `transactions.occurredAt` is ISO local text, so `>= '2026-08-01' AND < '2026-09-01'`
 * is an ordinary string comparison that picks out exactly the days of that month.
 *
 * It works because the date part is FIXED WIDTH and each bound is exactly that part:
 *  - the upper bound is settled inside the first ten characters — `'2026-08-31T23:59:59'` is below
 *    `'2026-09-01'` on the month digit, `8 < 9`, so the last instant of the 31st is inside and the
 *    comparison never reaches the `'T'`;
 *  - the lower bound is settled by the prefix rule — `'2026-08-01T00:00:00'` starts with
 *    `'2026-08-01'` and is longer, and a string sorts after any prefix of itself.
 *
 * Where `'T'` falls in the character set has nothing to do with either. The constant width does.
 */
data class MonthRange(val startInclusive: String, val endExclusive: String)
