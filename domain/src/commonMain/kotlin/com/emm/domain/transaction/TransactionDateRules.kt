package com.emm.domain.transaction

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal fun ensureNotFutureDated(occurredAt: LocalDateTime, clock: Clock, zone: TimeZone) {
    val today = clock.now().toLocalDateTime(zone).date
    if (occurredAt.date > today) {
        throw DomainException.ValidationError(
            "Transaction date ${occurredAt.date} is after today ($today)",
            ValidationCode.DateInTheFuture,
        )
    }
}
