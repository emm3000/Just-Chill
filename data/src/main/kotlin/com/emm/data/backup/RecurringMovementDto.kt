package com.emm.data.backup

import com.emm.data.shared.enumValueOrNull
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.parsePeriodKey
import com.emm.domain.recurring.periodKey
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionType
import kotlinx.serialization.Serializable

internal const val BACKUP_RECURRING_SINCE_VERSION: Int = 3

@Serializable
data class RecurringMovementDto(
    val recurringMovementId: String,
    val name: String,
    val type: String,
    val amountCents: Long?,
    val description: String,
    val categoryId: String?,
    val accountId: String,
    val frequency: String,
    val dayOfMonth: Int,
    val isActive: Boolean,
    val lastConfirmedPeriod: String?,
    val createdAt: Long,
)

fun RecurringMovementDto.toEntityOrNull(): RecurringMovement? {
    val parsedType: TransactionType? = enumValueOrNull<TransactionType>(type)
    val parsedFrequency: Frequency? = enumValueOrNull<Frequency>(frequency)
    return if (parsedType == null || parsedFrequency == null) {
        null
    } else {
        RecurringMovement(
            id = RecurringMovementId(recurringMovementId),
            name = name,
            type = parsedType,
            amount = amountCents?.let(::Money),
            description = description,
            categoryId = categoryId?.let(::CategoryId),
            accountId = AccountId(accountId),
            frequency = parsedFrequency,
            dayOfMonth = dayOfMonth,
            isActive = isActive,
            lastConfirmedPeriod = lastConfirmedPeriod?.let(::parsePeriodKey)?.let(::periodKey),
            createdAt = createdAt,
        )
    }
}

fun RecurringMovement.toDto() = RecurringMovementDto(
    recurringMovementId = id.value,
    name = name,
    type = type.name,
    amountCents = amount?.cents,
    description = description,
    categoryId = categoryId?.value,
    accountId = accountId.value,
    frequency = frequency.name,
    dayOfMonth = dayOfMonth,
    isActive = isActive,
    lastConfirmedPeriod = lastConfirmedPeriod,
    createdAt = createdAt,
)
