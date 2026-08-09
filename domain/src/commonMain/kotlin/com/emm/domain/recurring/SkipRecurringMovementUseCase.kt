package com.emm.domain.recurring

import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

class SkipRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

    /**
     * Settles [yearMonth] for a template without booking a transaction.
     *
     * Catching up needs an escape hatch. Periods are confirmed oldest-first, so a month the user
     * genuinely did not pay — the gym they cancelled, the salary that did not arrive — would
     * otherwise block every later month behind it forever.
     *
     * It moves the same high-water mark [ConfirmRecurringMovementUseCase] moves, and carries the
     * same monotonic guard, so skipping cannot rewind past periods into existence either.
     */
    suspend operator fun invoke(templateId: RecurringMovementId, yearMonth: YearMonth) {
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

        repository.skip(templateId, periodKey(yearMonth))
    }
}
