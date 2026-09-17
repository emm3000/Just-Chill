package com.emm.data.recurring

import com.emm.data.Recurring_movements
import com.emm.data.SelectAllWithDetails
import com.emm.data.shared.enumValueOrNull
import com.emm.justchill.core.domain.recurring.Frequency
import com.emm.justchill.core.domain.recurring.RecurringMovement
import com.emm.justchill.core.domain.recurring.RecurringMovementDetails
import com.emm.justchill.core.domain.recurring.RecurringMovementInsert
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.RecurringMovementId
import com.emm.justchill.core.domain.transaction.TransactionType

fun Recurring_movements.asEntity() = RecurringMovementEntity(
    id = id,
    name = name,
    type = type,
    amount = amount,
    description = description,
    categoryId = categoryId,
    accountId = accountId,
    frequency = frequency,
    dayOfMonth = dayOfMonth,
    isActive = isActive,
    lastConfirmedPeriod = lastConfirmedPeriod,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Recurring_movements>.asEntity() = map(Recurring_movements::asEntity)

fun RecurringMovementEntity.asExternalModelOrNull(): RecurringMovement? {
    val parsedType = enumValueOrNull<TransactionType>(type)
    val parsedFrequency = enumValueOrNull<Frequency>(frequency)
    if (parsedType == null || parsedFrequency == null) return null
    return RecurringMovement(
        id = RecurringMovementId(id),
        name = name,
        type = parsedType,
        amount = amount?.let { Money(it) },
        description = description,
        categoryId = categoryId?.let(::CategoryId),
        accountId = AccountId(accountId),
        frequency = parsedFrequency,
        dayOfMonth = dayOfMonth.toInt(),
        isActive = isActive != 0L,
        lastConfirmedPeriod = lastConfirmedPeriod,
        createdAt = createdAt,
    )
}

fun List<RecurringMovementEntity>.asExternalModel() = mapNotNull(RecurringMovementEntity::asExternalModelOrNull)

fun SelectAllWithDetails.asExternalModelOrNull(): RecurringMovementDetails? {
    val parsedType = enumValueOrNull<TransactionType>(type) ?: return null
    return RecurringMovementDetails(
        id = id,
        name = name,
        type = parsedType,
        amount = amount?.let { Money(it) },
        categoryName = categoryName,
        categoryColor = categoryColor,
        accountName = accountName,
        dayOfMonth = dayOfMonth.toInt(),
        isActive = isActive != 0L,
    )
}

fun RecurringMovementInsert.toPersistParams(id: String, now: Long) = RecurringMovementEntity(
    id = id,
    name = name,
    type = type.name,
    amount = amount?.cents,
    description = description,
    categoryId = categoryId?.value,
    accountId = accountId.value,
    frequency = Frequency.Monthly.name,
    dayOfMonth = dayOfMonth.toLong(),
    isActive = if (isActive) 1L else 0L,
    lastConfirmedPeriod = null,
    createdAt = now,
    updatedAt = now,
)
