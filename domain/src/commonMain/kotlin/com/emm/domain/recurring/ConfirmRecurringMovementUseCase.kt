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

    /**
     * Confirms a recurring movement for [yearMonth], creating the transaction it stands for.
     *
     * Logic:
     * 1. find(id) → NotFound if absent
     * 2. monotonic guard: [yearMonth] must be newer than the template's high-water mark
     * 3. resolve amount: template.amount if non-null, else callerAmount (null → ValidationError)
     * 4. validate resolved amount > 0
     * 5. build TransactionInsert — occurredAt = the period's own due day; `:data` stamps the timestamps
     * 6. repo.confirm(insert, id, periodKey) — atomic in the data layer
     *
     * The transaction is dated on [yearMonth]'s due day, not on today. Catching up on July while
     * it is August has to book July's rent in July, or Home and Reporte disagree about the month
     * the money moved. It cannot land in the future either: [pendingPeriods] does not offer the
     * current month until its due day has arrived.
     *
     * The guard is monotonic rather than an equality check because [RecurringMovement.lastConfirmedPeriod]
     * is a high-water mark. Accepting a period at or before it would strand every period in
     * between — confirm August with July still owed and July vanishes. Callers settle oldest-first.
     *
     * Does NOT call CreateTransactionUseCase (Option A atomicity).
     *
     * No clock and no timezone: the due day is a calendar fact derived from [yearMonth] and the
     * template's `dayOfMonth`, and the value it produces carries no zone either.
     */
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
            description = template.description,
            categoryId = template.categoryId,
            occurredAt = LocalDateTime(template.dueDate(yearMonth), MIDNIGHT),
            accountId = template.accountId,
        )

        repository.confirm(insert, templateId, periodKey(yearMonth))
    }
}

/**
 * The template's due day within [yearMonth], clamped to short months.
 *
 * A confirmed movement lands at midnight on that day. That is pre-existing behaviour and it is
 * unchanged here: the template knows which day it falls on and nothing about what time.
 */
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
