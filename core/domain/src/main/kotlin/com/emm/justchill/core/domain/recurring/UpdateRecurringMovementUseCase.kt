package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.RecurringMovementId

class UpdateRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

    suspend operator fun invoke(id: RecurringMovementId, insert: RecurringMovementInsert) {
        validateInsert(insert)
        repository.update(id, insert)
    }
}
