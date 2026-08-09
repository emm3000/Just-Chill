package com.emm.domain.recurring

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class GetPendingRecurringMovementsUseCase(
    private val repository: RecurringMovementRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {

    /**
     * Every period every active template still owes as of [today], oldest period first.
     *
     * The caller supplies [today] so the clock is never read internally — testable without a fake
     * clock.
     *
     * Ordering matters: [ConfirmRecurringMovementUseCase] only accepts a period newer than the
     * template's high-water mark, so a template's own periods must be settled oldest-first. Across
     * templates the order is by period, then by name, so the list reads as a chronological queue.
     */
    operator fun invoke(today: LocalDate): Flow<List<PendingRecurring>> = repository.allActive().map { movements ->
        movements
            .flatMap { movement ->
                pendingPeriods(movement, today, timeZone).map { PendingRecurring(movement, it) }
            }
            .sortedWith(compareBy({ it.period }, { it.movement.name }))
    }
}
