package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.RecurringMovementId
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode

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
