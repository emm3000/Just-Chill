package com.emm.domain.recurring

import com.emm.domain.shared.YearMonth

data class PendingRecurring(val movement: RecurringMovement, val period: YearMonth)
