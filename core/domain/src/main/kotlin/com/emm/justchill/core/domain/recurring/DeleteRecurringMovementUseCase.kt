package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.RecurringMovementId
import com.emm.justchill.core.domain.shared.error.DomainException

class DeleteRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

    suspend operator fun invoke(id: RecurringMovementId) {
        repository.find(id) ?: throw DomainException.NotFound("RecurringMovement(${id.value})")
        repository.delete(id)
    }
}
