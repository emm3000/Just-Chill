package com.emm.domain.recurring

import com.emm.domain.shared.YearMonth

/**
 * One recurring movement owed for one specific [period].
 *
 * A template can owe several months at once — the pending list is per period, not per template, so
 * catching up after a gap surfaces every missed month instead of only the current one.
 */
data class PendingRecurring(val movement: RecurringMovement, val period: YearMonth)
