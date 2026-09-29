package com.emm.justchill.core.domain.shared

import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal fun ensureNotFutureDated(occurredAt: LocalDateTime, clock: Clock, zone: TimeZone) {
    val today = clock.now().toLocalDateTime(zone).date
    if (occurredAt.date > today) {
        throw DomainException.ValidationError(
            "Date ${occurredAt.date} is after today ($today)",
            ValidationCode.DateInTheFuture,
        )
    }
}
