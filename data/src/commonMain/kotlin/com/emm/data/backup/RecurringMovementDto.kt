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

/**
 * The first format version that carried recurring movements, and therefore the oldest file an import
 * is allowed to sweep that table for.
 *
 * **A literal, and never [BACKUP_SCHEMA_VERSION].** The two are equal today and the equality is a
 * coincidence of timing: the moment the format grows a version 4, a `>= BACKUP_SCHEMA_VERSION` gate
 * would stop restoring the templates in every version 3 file ever written — silently, since the
 * import would read the file, skip the table, and report success. This number is a fact about which
 * files carry templates; it cannot move.
 *
 * The comparison is `>=` rather than `==` for the same reason: every version from here on carries
 * them, so a newer file must not fall through to the pre-v3 behaviour either.
 */
internal const val BACKUP_RECURRING_SINCE_VERSION: Int = 3

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
 * `DefaultBackupRepositoryImportTest` pins one each, because a single test cannot fail in both
 * directions — and it pins them over the whole round trip, which is the only place the claim holds.
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

/**
 * Null when the file carries a template this app cannot interpret — a `type` or a `frequency` that is
 * not one of ours. The row is dropped rather than restored under a guessed enum.
 *
 * ### Why the drop, when a recurring row cannot skew a balance the way a transaction can
 *
 * `TransactionDto.toEntityOrNull` drops a row because writing it makes the Home balance disagree with
 * the visible ledger. A template with an unknown `type` cannot do that: `GetRecurringMonthlyTotals`
 * folds the same `RecurringMovementDetails` list the screen renders, and both come through
 * `asExternalModelOrNull`, which skips the row on exactly this input. It is invisible everywhere,
 * consistently.
 *
 * It is dropped anyway, because "invisible" is not "harmless". The row still holds its id, still
 * pushes to the server, and — the one with teeth — still counts in `countLiveByAccount`, which is
 * what `DeleteAccountUseCase` reads: the owner is told the account still has recurring movements
 * while the recurring screen shows none, with no way to reach the row that is blocking them. A file
 * this app wrote can never contain that value; a hand-edited one can.
 *
 * ### `lastConfirmedPeriod` is repaired rather than dropped, and that asymmetry is the decision
 *
 * A garbage period key is the third failure this function was asked about, and it is real:
 * `ensureNotSettled` compares `lastConfirmedPeriod` to the period being confirmed as **plain
 * strings**, which is only chronological while the value is zero-padded `"YYYY-MM"`. `"julio"` sorts
 * above every real key, so every confirmation is refused as already-settled, while
 * [pendingPeriods][com.emm.domain.recurring.pendingPeriods] parses the same value, gets null, and
 * goes on listing those months as owed. The user is shown months to confirm and cannot confirm any of
 * them.
 *
 * So it is not left alone. It is re-encoded through `parsePeriodKey` + `periodKey` — the same policy
 * `restore(dto: TransactionDto)` applies to `occurredAt`, canonical bytes in the column no matter
 * which shape the file used — which also repairs the near-miss the parser accepts and the comparison
 * does not: `"2026-7"` reads as July and sorts above `"2026-08"`.
 *
 * What it is NOT is a reason to drop the template. Unparseable becomes **null**, which the column
 * already means "never settled", and the cost is bounded and repairable: the app offers the settled
 * months again, and `SkipRecurringMovementUseCase` puts the mark back in one tap per month without
 * writing money the user did not enter. Dropping instead would throw away the name, the amount, the
 * account and the day — everything the file did carry correctly — over one field the app itself can
 * rebuild. An unknown enum has no such middle answer: there is no safe default for `type`, and
 * coercing Income to Spend corrupts the ledger it mints into.
 */
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
