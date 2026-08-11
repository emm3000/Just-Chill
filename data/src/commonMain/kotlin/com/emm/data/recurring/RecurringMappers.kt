package com.emm.data.recurring

import com.emm.data.Recurring_movements
import com.emm.data.SelectAllWithDetails
import com.emm.data.shared.enumValueOrNull
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementDetails
import com.emm.domain.recurring.RecurringMovementInsert
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
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
// Rows can originate from remote devices; the type and frequency columns are untrusted.
// Unknown enum value → skip the row (return null), never coerce to a default.
// Coercing Income→Spend or vice-versa would silently corrupt financial totals.
// Throwing is no longer acceptable: unhandled exceptions in list Flows propagate to Main and
// crash-loop the app on launch; skipped rows resurface once the app version knows the value.
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

// SQLDelight JOIN row → domain RecurringMovementDetails
// NOTE: isActive is plain INTEGER (not AS Boolean) — keep != 0L mapping (see design Decision 3).
// NOTE: accountName may technically be null in the generated type (LEFT JOIN) but accountId FK
//       is NOT NULL + ON DELETE RESTRICT, so null is a data-drift edge case; coalesce to "".
// Unknown type → skip row (return null). Same remote-trust reasoning as asExternalModelOrNull above.
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

// Domain insert → flat params for LocalDataSource.
// [now] is passed in rather than read here: a mapper that reads a clock is a pure function that
// is not, and the caller already holds the write's single timestamp.
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
