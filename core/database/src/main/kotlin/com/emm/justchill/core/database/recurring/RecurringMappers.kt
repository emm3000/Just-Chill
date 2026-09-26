package com.emm.justchill.core.database.recurring

import com.emm.justchill.core.database.Recurring_movements
import com.emm.justchill.core.database.shared.enumValueOrNull
import com.emm.justchill.core.domain.recurring.Frequency
import com.emm.justchill.core.domain.recurring.RecurringMovement
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.RecurringMovementId
import com.emm.justchill.core.domain.transaction.TransactionType

fun Recurring_movements.asEntity(): RecurringMovementEntity = RecurringMovementEntity(
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

fun List<Recurring_movements>.asEntity(): List<RecurringMovementEntity> = map(Recurring_movements::asEntity)

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

fun List<RecurringMovementEntity>.asExternalModel(): List<RecurringMovement> =
    mapNotNull(RecurringMovementEntity::asExternalModelOrNull)
