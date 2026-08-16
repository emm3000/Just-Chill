package com.emm.domain.recurring

import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

class SkipRecurringMovementUseCase(private val repository: RecurringMovementRepository) {

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
