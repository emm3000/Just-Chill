@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import kotlin.uuid.Uuid

/**
 * What the user is asking to record. [date] is the day the money moved — the only date here that
 * is theirs.
 *
 * `createdAt` and `updatedAt` are deliberately absent. They are storage metadata, not user input:
 * the row's age and the key LWW conflict resolution reads. `:data` stamps them at the write, which
 * is also where `update` and `softDelete` have always stamped them.
 *
 * They used to live here as `= currentTimeInMillis()` default arguments, which made this data class
 * read the wall clock on construction — hidden I/O in an entity, uninjectable and untestable. Worse,
 * it timed the row from whenever the object happened to be built rather than from the write, the
 * same shape as the bug that dated a transaction when the screen opened instead of when it saved.
 */
data class TransactionInsert(
    val id: TransactionId = TransactionId(Uuid.random().toString()),
    val type: TransactionType,
    val amount: Money,
    val description: String,
    val categoryId: CategoryId?,
    val date: Long,
    val accountId: AccountId,
)
