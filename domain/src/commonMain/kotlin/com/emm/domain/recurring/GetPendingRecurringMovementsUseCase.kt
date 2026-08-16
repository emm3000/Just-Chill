package com.emm.domain.recurring

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class GetPendingRecurringMovementsUseCase(
    private val repository: RecurringMovementRepository,
    private val timeZone: TimeZone,
) {

    operator fun invoke(today: LocalDate): Flow<List<PendingRecurring>> = repository.allActive().map { movements ->
        movements
            .flatMap { movement ->
                pendingPeriods(movement, today, timeZone).map { PendingRecurring(movement, it) }
            }
            .sortedWith(compareBy({ it.period }, { it.movement.name }))
    }
}
