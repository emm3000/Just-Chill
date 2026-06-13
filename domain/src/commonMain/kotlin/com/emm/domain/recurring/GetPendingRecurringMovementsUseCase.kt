package com.emm.domain.recurring

import com.emm.domain.shared.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class GetPendingRecurringMovementsUseCase(private val repository: RecurringMovementRepository) {

    /**
     * Returns active recurring movements that are due for [yearMonth] as of [today].
     *
     * The caller supplies [today] so the clock is never read internally — testable
     * without fakes for the clock.
     *
     * Does NOT produce results for past or future months — the caller is responsible
     * for passing the current period. No retroactive catch-up.
     */
    operator fun invoke(today: LocalDate, yearMonth: YearMonth): Flow<List<RecurringMovement>> =
        repository.allActive().map { list ->
            list.filter { rm -> isPending(rm, yearMonth, today) }
        }
}
