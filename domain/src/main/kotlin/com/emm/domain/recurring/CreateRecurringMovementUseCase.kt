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

private fun check(condition: Boolean, error: DomainException) {
    if (!condition) throw error
}

internal fun validateInsert(insert: RecurringMovementInsert) {
    check(insert.name.isNotBlank(), DomainException.ValidationError("Name cannot be empty"))
    check(insert.accountId.value.isNotBlank(), DomainException.ValidationError("Account ID cannot be empty"))
    check(
        insert.dayOfMonth in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH,
        DomainException.ValidationError("Day of month must be between 1 and 31, got ${insert.dayOfMonth}"),
    )
    val amount = insert.amount
    check(
        amount == null || amount.cents > 0,
        DomainException.ValidationError("Amount must be greater than zero when provided; use null for variable amount"),
    )
}
