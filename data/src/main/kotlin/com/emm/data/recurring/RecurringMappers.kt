package com.emm.data.recurring

import com.emm.data.Recurring_movements
import com.emm.data.SelectAllWithDetails
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementDetails
import com.emm.domain.recurring.RecurringMovementInsert
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.currentTimeInMillis
import com.emm.domain.transaction.TransactionType

// SQLDelight row → Entity (stays within data source)
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

// Entity → Domain
// TransactionType.valueOf and Frequency.valueOf intentionally let any unknown value throw
// so safeDbCall translates it to DomainException.DatabaseError.
// Silent coercion (e.g. Income→Spend) would corrupt financial totals.
fun RecurringMovementEntity.asExternalModel() = RecurringMovement(
    id = RecurringMovementId(id),
    name = name,
    type = TransactionType.valueOf(type),
    amount = amount?.let { Money(it) },
    description = description,
    categoryId = categoryId?.let(::CategoryId),
    accountId = AccountId(accountId),
    frequency = Frequency.valueOf(frequency),
    dayOfMonth = dayOfMonth.toInt(),
    isActive = isActive != 0L,
    lastConfirmedPeriod = lastConfirmedPeriod,
)

fun List<RecurringMovementEntity>.asExternalModel() = map(RecurringMovementEntity::asExternalModel)

// SQLDelight JOIN row → domain RecurringMovementDetails
// NOTE: isActive is plain INTEGER (not AS Boolean) — keep != 0L mapping (see design Decision 3).
// NOTE: accountName may technically be null in the generated type (LEFT JOIN) but accountId FK
//       is NOT NULL + ON DELETE RESTRICT, so null is a data-drift edge case; coalesce to "".
fun SelectAllWithDetails.asExternalModel() = RecurringMovementDetails(
    id = id,
    name = name,
    type = TransactionType.valueOf(type),
    amount = amount?.let { Money(it) },
    categoryName = categoryName,
    categoryColor = categoryColor,
    accountName = accountName,
    dayOfMonth = dayOfMonth.toInt(),
    isActive = isActive != 0L,
)

// Domain insert → flat params for LocalDataSource
fun RecurringMovementInsert.toPersistParams(id: String): RecurringMovementEntity {
    val now = currentTimeInMillis()
    return RecurringMovementEntity(
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
}
