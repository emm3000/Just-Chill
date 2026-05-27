package com.emm.domain.recurring

import kotlinx.coroutines.flow.Flow

class GetAllRecurringMovementsUseCase(private val repository: RecurringMovementRepository) {

    operator fun invoke(): Flow<List<RecurringMovement>> = repository.all()
}
