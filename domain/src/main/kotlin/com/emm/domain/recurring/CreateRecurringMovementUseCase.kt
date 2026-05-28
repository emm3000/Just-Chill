package com.emm.domain.recurring

import com.emm.domain.shared.error.DomainException

private const val MIN_DAY_OF_MONTH = 1
private const val MAX_DAY_OF_MONTH = 31

class CreateRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

    suspend operator fun invoke(insert: RecurringMovementInsert) {
        validateInsert(insert)
        repository.create(insert)
    }
}

internal fun validateInsert(insert: RecurringMovementInsert) {
    ensure(insert.name.isNotBlank(), DomainException.ValidationError("Name cannot be empty"))
    ensure(insert.accountId.value.isNotBlank(), DomainException.ValidationError("Account ID cannot be empty"))
    ensure(
        insert.dayOfMonth in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH,
        DomainException.ValidationError(
            "Day of month must be between $MIN_DAY_OF_MONTH and $MAX_DAY_OF_MONTH, got ${insert.dayOfMonth}",
        ),
    )
    val amount = insert.amount
    ensure(
        amount == null || amount.cents > 0,
        DomainException.ValidationError("Amount must be greater than zero when provided; use null for variable amount"),
    )
}
