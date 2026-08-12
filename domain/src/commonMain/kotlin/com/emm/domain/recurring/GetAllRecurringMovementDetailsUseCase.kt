package com.emm.domain.recurring

import kotlinx.coroutines.flow.Flow

/** Thin delegate over [RecurringMovementRepository.allWithDetails]. */
class GetAllRecurringMovementDetailsUseCase(private val repository: RecurringMovementRepository) {

    operator fun invoke(): Flow<List<RecurringMovementDetails>> = repository.allWithDetails()
}
