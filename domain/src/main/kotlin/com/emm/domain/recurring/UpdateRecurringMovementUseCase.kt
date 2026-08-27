package com.emm.domain.recurring

import com.emm.domain.shared.RecurringMovementId

class UpdateRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

    suspend operator fun invoke(id: RecurringMovementId, insert: RecurringMovementInsert) {
        validateInsert(insert)
        repository.update(id, insert)
    }
}
