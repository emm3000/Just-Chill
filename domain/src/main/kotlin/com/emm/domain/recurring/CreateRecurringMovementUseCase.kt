package com.emm.domain.recurring

import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.currentTimeInMillis
import com.emm.domain.shared.error.DomainException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(insert: RecurringMovementInsert) {
        validateInsert(insert)
        repository.create(insert)
    }
}

internal fun validateInsert(insert: RecurringMovementInsert) {
    if (insert.name.isBlank()) {
        throw DomainException.ValidationError("Name cannot be empty")
    }
    if (insert.accountId.value.isBlank()) {
        throw DomainException.ValidationError("Account ID cannot be empty")
    }
    if (insert.dayOfMonth !in 1..31) {
        throw DomainException.ValidationError("Day of month must be between 1 and 31, got ${insert.dayOfMonth}")
    }
    val amount = insert.amount
    if (amount != null && amount.cents <= 0) {
        throw DomainException.ValidationError("Amount must be greater than zero when provided; use null for variable amount")
    }
}
