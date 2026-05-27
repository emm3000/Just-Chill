package com.emm.domain.recurring

import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.shared.currentTimeInMillis
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ConfirmRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

    /**
     * Confirms a recurring movement for [yearMonth].
     *
     * Logic:
     * 1. find(id) → NotFound if absent
     * 2. idempotency guard: if lastConfirmedPeriod == periodKey(yearMonth) → ValidationError
     * 3. resolve amount: template.amount if non-null, else callerAmount (null → ValidationError)
     * 4. validate resolved amount > 0
     * 5. build TransactionInsert with today's epoch millis as date
     * 6. repo.confirm(insert, id, periodKey) — atomic in data layer
     *
     * Does NOT call CreateTransactionUseCase (Option A atomicity).
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        templateId: RecurringMovementId,
        yearMonth: YearMonth,
        today: LocalDate,
        callerAmount: Money?,
    ) {
        val template = repository.find(templateId)
            ?: throw DomainException.NotFound("RecurringMovement(${templateId.value})")

        val currentPeriod = periodKey(yearMonth)
        if (template.lastConfirmedPeriod == currentPeriod) {
            throw DomainException.ValidationError(
                "Template '${template.name}' already confirmed for period $currentPeriod",
            )
        }

        val resolvedAmount: Money = when {
            template.amount != null -> template.amount
            callerAmount != null -> callerAmount
            else -> throw DomainException.ValidationError(
                "Amount is required for variable-amount template '${template.name}'",
            )
        }

        if (resolvedAmount.cents <= 0) {
            throw DomainException.ValidationError(
                "Confirmed amount must be greater than zero, got ${resolvedAmount.cents} cents",
            )
        }

        val dateMillis: Long = today.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val now = currentTimeInMillis()
        val insert = TransactionInsert(
            id = TransactionId(Uuid.random().toString()),
            type = template.type,
            amount = resolvedAmount,
            description = template.description,
            categoryId = template.categoryId,
            date = dateMillis,
            accountId = template.accountId,
            updatedAt = now,
            createdAt = now,
        )

        repository.confirm(insert, templateId.value, currentPeriod)
    }
}
