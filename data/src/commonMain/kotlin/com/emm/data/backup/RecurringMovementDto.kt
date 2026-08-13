package com.emm.data.backup

import com.emm.domain.recurring.RecurringMovement
import kotlinx.serialization.Serializable

/**
 * A recurring movement — a template, not a movement — as schema version 3 writes it.
 *
 * Most field names mirror the `recurring_movements` columns; [amountCents] and [recurringMovementId]
 * deliberately do not, because **the file is read by people and the columns are not.** Where the two
 * pull apart, readability of the document wins — see each field for its own case, and do not "fix"
 * either back to its column name.
 *
 * For [amountCents] specifically: the column and `transactions.amount`
 * are both INTEGER cents behind a `Money`, and [TransactionDto] already diverged from its own column
 * name to say so out loud, because a backup file is read by humans and by future importers that have
 * no `Money` to ask. `"amount": 1500` sitting beside `"amountCents": 1500` in one document does not
 * read as one unit in two tables — it reads as two units, and a reader who guesses wrong is off by a
 * factor of 100 in the user's only copy of their data. The column name is an internal detail that
 * can still be renamed; the file format is a public contract, frozen the moment a build ships. This
 * DTO is consistent with the frozen half.
 *
 * Two of the twelve fields are behaviour rather than storage, and they lose data in **opposite**
 * directions if they go missing. Neither is obvious from its name, so both are spelled out:
 *
 *  - [lastConfirmedPeriod] is the high-water mark. Drop it and a restored template owes every month
 *    the user already settled — the app re-mints an income or a rent the ledger already holds.
 *  - [createdAt] is the floor of the catch-up window. Drop it and every restore path stamps the
 *    restore instant instead, which raises the floor to the restore month and makes every period the
 *    template still owes disappear. Silently: no error, no row, nothing on screen to notice.
 *
 * `DefaultBackupRepositoryTest` pins one each, because a single test cannot fail in both directions.
 *
 * [type] and [frequency] carry enum *names* rather than the enums, for the same reason
 * [TransactionDto.type] does: a backup file is untrusted input, and a value this build does not know
 * must be a row it can skip rather than a file it reports as corrupt.
 */
@Serializable
data class RecurringMovementDto(
    /**
     * Named for its entity, not `id` — the same rule [amountCents] follows.
     *
     * The other three collections in this document spell their key `accountId`, `categoryId`,
     * `transactionId`, and this object carries two of those alongside its own. A bare `id` next to
     * `categoryId` and `accountId` makes the reader infer which entity it identifies; naming it
     * removes the question.
     */
    val recurringMovementId: String,
    val name: String,
    val type: String,
    /**
     * Cents, spelled out — see the class doc for why this one does not carry the column's name.
     *
     * Nullable, because the column is: a template may exist without a fixed amount, and "no amount
     * agreed" is not `0`.
     */
    val amountCents: Long?,
    val description: String,
    val categoryId: String?,
    val accountId: String,
    /**
     * One member (`Monthly`) today, and the column carries a DEFAULT that `update:` never writes.
     *
     * Exported anyway: this is a file format, not a snapshot of today's cardinality. A file written
     * now has to still say what it meant after a second frequency exists.
     */
    val frequency: String,
    val dayOfMonth: Int,
    val isActive: Boolean,
    /** The newest period ("YYYY-MM") confirmed or skipped, or null when the template never was. */
    val lastConfirmedPeriod: String?,
    /** Epoch millis, and the template's OWN — never the export's or the restore's. */
    val createdAt: Long,
)

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
