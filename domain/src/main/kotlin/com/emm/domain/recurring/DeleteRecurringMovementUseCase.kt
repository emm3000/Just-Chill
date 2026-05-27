package com.emm.domain.recurring

import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.error.DomainException

class DeleteRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

    suspend operator fun invoke(id: RecurringMovementId) {
        repository.find(id) ?: throw DomainException.NotFound("RecurringMovement(${id.value})")
        repository.delete(id)
    }
}
