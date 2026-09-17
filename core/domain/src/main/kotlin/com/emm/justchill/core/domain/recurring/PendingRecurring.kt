package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.YearMonth

data class PendingRecurring(val movement: RecurringMovement, val period: YearMonth)
