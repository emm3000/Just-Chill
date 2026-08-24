package com.emm.domain.recurring

import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.YearMonth
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.domain.transaction.TransactionInsert
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ConfirmRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(templateId: RecurringMovementId, yearMonth: YearMonth, callerAmount: Money?) {
        val template = repository.find(templateId)
            ?: throw DomainException.NotFound("RecurringMovement(${templateId.value})")

        val mark: YearMonth? = template.lastConfirmedPeriod?.let(::parsePeriodKey)
        ensure(
            mark == null || yearMonth > mark,
            DomainException.ValidationError(
                "Template '${template.name}' is already settled through ${template.lastConfirmedPeriod}",
                ValidationCode.RecurringAlreadyConfirmed,
            ),
        )

        val resolvedAmount: Money = resolveAmount(template, callerAmount)
        ensure(
            resolvedAmount.cents > 0,
            DomainException.ValidationError(
                "Confirmed amount must be greater than zero, got ${resolvedAmount.cents} cents",
                ValidationCode.AmountMustBePositive,
            ),
        )

        val insert = TransactionInsert(
            id = TransactionId(Uuid.random().toString()),
            type = template.type,
            amount = resolvedAmount,
            description = template.description.ifBlank { template.name },
            categoryId = template.categoryId,
            occurredAt = LocalDateTime(template.dueDate(yearMonth), MIDNIGHT),
            accountId = template.accountId,
        )

        repository.confirm(insert, templateId, periodKey(yearMonth))
    }
}

private fun RecurringMovement.dueDate(yearMonth: YearMonth): LocalDate {
    val dueDay = when (frequency) {
        Frequency.Monthly -> effectiveDueDay(dayOfMonth, yearMonth)
    }
    return LocalDate(yearMonth.year, yearMonth.month, dueDay)
}

private val MIDNIGHT = LocalTime(0, 0)

private fun resolveAmount(template: RecurringMovement, callerAmount: Money?): Money = template.amount
    ?: callerAmount
    ?: throw DomainException.ValidationError(
        "Amount is required for variable-amount template '${template.name}'",
        ValidationCode.AmountRequired,
    )
