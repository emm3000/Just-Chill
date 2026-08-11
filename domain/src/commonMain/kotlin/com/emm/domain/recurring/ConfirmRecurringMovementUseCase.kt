package com.emm.domain.recurring

import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.YearMonth
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.domain.transaction.TransactionInsert
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ConfirmRecurringMovementUseCase(
    private val repository: RecurringMovementRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {

    /**
     * Confirms a recurring movement for [yearMonth], creating the transaction it stands for.
     *
     * Logic:
     * 1. find(id) → NotFound if absent
     * 2. monotonic guard: [yearMonth] must be newer than the template's high-water mark
     * 3. resolve amount: template.amount if non-null, else callerAmount (null → ValidationError)
     * 4. validate resolved amount > 0
     * 5. build TransactionInsert — date = the period's own due day; `:data` stamps the timestamps
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
     * [timeZone] is injected so tests can assert exact epoch millis without hidden clock reads.
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
            date = template.dueDateMillis(yearMonth, timeZone),
            accountId = template.accountId,
        )

        repository.confirm(insert, templateId, periodKey(yearMonth))
    }
}

/** Start of the template's due day within [yearMonth], clamped to short months. */
private fun RecurringMovement.dueDateMillis(yearMonth: YearMonth, timeZone: TimeZone): Long {
    val dueDay = when (frequency) {
        Frequency.Monthly -> effectiveDueDay(dayOfMonth, yearMonth)
    }
    return LocalDate(yearMonth.year, yearMonth.month, dueDay).atStartOfDayIn(timeZone).toEpochMilliseconds()
}

private fun resolveAmount(template: RecurringMovement, callerAmount: Money?): Money = template.amount
    ?: callerAmount
    ?: throw DomainException.ValidationError(
        "Amount is required for variable-amount template '${template.name}'",
        ValidationCode.AmountRequired,
    )
