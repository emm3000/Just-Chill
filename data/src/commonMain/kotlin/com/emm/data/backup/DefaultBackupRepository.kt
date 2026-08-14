package com.emm.data.backup

import com.emm.data.EmmDatabaseData
import com.emm.data.account.asEntity
import com.emm.data.account.asExternalModel
import com.emm.data.category.asEntity
import com.emm.data.category.asExternalModel
import com.emm.data.recurring.asEntity
import com.emm.data.recurring.asExternalModel
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.nowMillis
import com.emm.data.shared.safeDbCall
import com.emm.data.shared.toOccurredAtText
import com.emm.data.transaction.asEntity
import com.emm.data.transaction.asExternalModel
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.ImportStats
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

private val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

private val importJson = Json {
    ignoreUnknownKeys = true
}

private const val SCHEMA_VERSION_KEY = "schemaVersion"

class DefaultBackupRepository(private val db: EmmDatabaseData, private val clock: Clock) : BackupRepository {

    /**
     * The whole ledger as ONE snapshot — four tables read inside a single transaction.
     *
     * **This used to be four independent `Flow.first()` reads through the four repository
     * interfaces**, and a write landing between any two of them produced a file describing a
     * database state that never existed: a transaction filed under a category the next read no
     * longer returned, a template whose account the file does not carry. Nothing in the format can
     * repair that afterwards, because the file is internally consistent — it is simply wrong.
     *
     * A SQLDelight transaction is a **synchronous** block, so `first()` cannot run inside one. That
     * is the whole reason the reads are direct `executeAsList()` calls against [db] rather than
     * calls through `TransactionRepository`, `CategoryRepository`, `AccountRepository` and
     * `RecurringMovementRepository`, which this class no longer takes at all.
     *
     * **It reverses an earlier decision, deliberately.** The recurring read used to be argued for on
     * the grounds that going through the domain interface kept the export uniform — three tables
     * mockable and not a fourth — and `RecurringMovementRepository.allLive()` existed for this one
     * caller. The uniformity survives; only its direction flipped. All four tables are now read the
     * same way, through [db], and the suite that used to mock the four collaborators runs against a
     * real in-memory database instead. `allLive()` went with the caller it was added for; the
     * `selectAllLive` statement it wrapped is what this reads, so paused templates still travel —
     * `selectActive` would drop them, which is data the owner still holds.
     *
     * The dispatcher hop is what `mapToList(ioDispatcher)` used to supply inside each
     * `LocalDataSource`; `executeAsList()` has none of its own, so without it the four queries would
     * run on the caller's dispatcher. [ioDispatcher] is referenced the way every other reader in
     * this module references it — it is a platform `expect val`, not a Koin binding.
     *
     * [safeDbCall] replaces the `catchAsDomainException()` the repository interfaces applied: reading
     * [db] directly took that translation off the path, and a raw SQLite exception reaching
     * `ProfileViewModel` would be reported to the user as the generic unknown failure.
     */
    override suspend fun exportToJson(exportedAt: Long, appVersion: String): String {
        val payload = safeDbCall {
            withContext(ioDispatcher) {
                db.transactionWithResult { db.snapshot(exportedAt, appVersion) }
            }
        }
        return exportJson.encodeToString(payload)
    }

    override suspend fun importFromJson(json: String): ImportStats {
        // Both halves of the decode, deliberately: `payload` is the current shape, and
        // `decoded.declaredVersion` is the version the FILE declared — the only thing that can tell
        // a v1/v2 file apart from a v3 one exported by a device with no templates. See
        // [DecodedBackup], and [BACKUP_RECURRING_SINCE_VERSION] for why the gate is a `>=` against a
        // frozen literal rather than against the current version.
        val decoded = decodePayload(json)
        val payload = decoded.payload
        val fileCarriesRecurring = decoded.declaredVersion >= BACKUP_RECURRING_SINCE_VERSION

        return safeDbCall {
            // One read, reused by the tombstone sweep and by every row restored after it — the
            // discipline the rest of :data already follows. It used to be `Clock.System.now()`,
            // which made this the only writer in the module a test could not pin, on the one path
            // that rewrites every row the user owns at once.
            val now = clock.nowMillis()
            var restoredTransactions = 0
            var restoredRecurring = 0
            db.transaction {
                // "Replace everything" expressed as tombstones instead of physical deletes.
                // Every live row is tombstoned first, then the backup brings back exactly what it
                // carries. Rows the file does not mention keep their tombstone, so the removal
                // pushes to the server and reaches the other devices — a physical DELETE left no
                // trace, and the next pull simply downloaded the rows again.
                //
                // RECURRING MOVEMENTS ARE THE ONE VERSIONED TABLE, both halves gated on the version
                // the FILE declared. Versions 1 and 2 had no templates to carry, so an emptiness
                // that came out of `toCurrent()` is the format's silence and not the owner's data:
                // sweeping on it deletes every template on the device with nothing able to put them
                // back. A v3 file with an empty array is the opposite claim — the owner has none —
                // and must sweep. Only the declared version separates the two; `recurringMovements`
                // being empty cannot, which is why the gate never reads it.
                //
                // The detach is NOT gated and never was: `restore(dto: CategoryDto, …)` clears the
                // category off any recurring row whose type disagrees with the one being restored,
                // on every version, sweep or no sweep. A tombstone is not a delete — the swept row
                // still holds its old (categoryId, type) pair and still makes SQLite refuse the
                // category's type change.
                db.transactionsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                if (fileCarriesRecurring) {
                    db.recurring_movementsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                }
                db.categoriesQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                db.accountsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)

                // ORDER MATTERS: a restored transaction references its account and category, and the
                // templates go LAST — that is the only position where `usableCategoryId` reads the
                // categories the FILE carries rather than the ones the import is mid-replacing. See
                // its own KDoc.
                payload.accounts.forEach { dto -> restore(dto, now) }
                payload.categories.forEach { dto -> restore(dto, now) }
                restoredTransactions = payload.transactions.count { dto -> restore(dto, now) }
                if (fileCarriesRecurring) {
                    restoredRecurring = payload.recurringMovements.count { dto -> restore(dto, now) }
                }
            }

            // The count is what LANDED, not what the file held — for both counted tables, and for
            // the same underlying reason. Reporting the file's own length would say "3 movimientos
            // importados" while one of them is nowhere on screen, and the balance would still
            // include its amount, because the totals aggregate reads the column and the mappers
            // that hide the row never run on it. `recurring` mirrors that exactly: a template with
            // an unknown type or frequency is dropped by `restore(dto: RecurringMovementDto)` — see
            // its own KDoc — and a dropped row still counts in `countLiveByAccount`, so reporting
            // the file's length would claim a template imported that the recurring screen can never
            // show and that still blocks an account deletion with nothing on screen to explain it.
            ImportStats(
                accounts = payload.accounts.size,
                categories = payload.categories.size,
                transactions = restoredTransactions,
                recurring = restoredRecurring,
            )
        }
    }

    /**
     * Reads the file at whichever format version it declares, and hands BOTH results back.
     *
     * The version is read FIRST, from the raw JSON, and only then is the payload decoded as the
     * shape that version actually had. Decoding optimistically and falling back would mean a v1
     * file was parsed by v2's rules first, which is exactly the mistake that makes an old file
     * report itself as corrupt.
     *
     * The version it dispatched on then travels out with the payload, in [DecodedBackup]. Returning
     * the payload alone would throw it away: every branch below ends in the current shape, so a v1
     * file, a v2 file and a v3 file written by a device with no templates all leave here identical.
     * [DecodedBackup.declaredVersion] is what keeps them apart — and it is deliberately not
     * recoverable from [ExportPayloadDto.schemaVersion], which each `toCurrent()` restamps to the
     * current version precisely because the payload IS current by then.
     *
     * `internal` rather than private so the compatibility suites can assert that a v1 and a v2 file
     * reach this boundary still declaring 1 and 2. Nothing outside the import path calls it, and
     * nothing observable to `importFromJson`'s caller would fail if the version were lost — which is
     * exactly why it has to be pinned here.
     */
    internal fun decodePayload(json: String): DecodedBackup {
        val root = parse { importJson.parseToJsonElement(json).jsonObject }
        val declaredVersion = schemaVersionOf(root)

        val payload = when (declaredVersion) {
            BACKUP_SCHEMA_VERSION -> parse {
                importJson.decodeFromJsonElement<ExportPayloadDto>(root)
            }

            // The version every backup file that exists today was written in — the format before it
            // carried recurring movements. They restore, and they always will — see BackupV2.kt.
            BACKUP_SCHEMA_VERSION_V2 -> parse {
                importJson.decodeFromJsonElement<ExportPayloadV2Dto>(root).toCurrent()
            }

            // Files already on the user's disk, written before a transaction's occurrence stopped
            // being an instant. They restore, and they always will — see BackupV1.kt.
            BACKUP_SCHEMA_VERSION_V1 -> parse {
                importJson.decodeFromJsonElement<ExportPayloadV1Dto>(root).toCurrent()
            }

            else -> throw DomainException.ValidationError(
                "Unsupported file version.",
                ValidationCode.BackupVersionUnsupported,
            )
        }
        return DecodedBackup(declaredVersion = declaredVersion, payload = payload)
    }

    /**
     * The format version the file declares.
     *
     * Absent means **version 1**, not "unsupported". Before this field was written, the DTO's own
     * default supplied the 1, so a file without the key is simply an old file — and telling the
     * user it was written by "una versión que esta app no puede leer" describes the opposite of
     * what happened.
     *
     * Present-but-not-a-number is a different answer: that is a malformed file, and it is reported
     * as one. Reading it goes through [parse] because `jsonPrimitive` THROWS when the value is an
     * object or an array — evaluating it outside the guard is how a `{"schemaVersion": {}}` file
     * escaped as a raw exception and reached the user as the generic failure instead of the
     * specific corrupt-file message.
     */
    private fun schemaVersionOf(root: JsonObject): Int {
        val declared = root[SCHEMA_VERSION_KEY] ?: return BACKUP_SCHEMA_VERSION_V1
        return parse { declared.jsonPrimitive.intOrNull } ?: throw invalidFile(cause = null)
    }

    /**
     * Anything the deserializer rejects is the same answer to the user: this file is not one of
     * ours. Both throw types are caught — kotlinx-serialization raises [SerializationException]
     * for a shape mismatch and [IllegalArgumentException] both for an enum value it does not know
     * and for a JSON element that is not the shape the accessor demanded.
     */
    @Suppress("SwallowedException")
    private inline fun <T> parse(block: () -> T): T = try {
        block()
    } catch (e: SerializationException) {
        throw invalidFile(e)
    } catch (e: IllegalArgumentException) {
        throw invalidFile(e)
    }

    private fun invalidFile(cause: Throwable?) = DomainException.ValidationError(
        "Invalid or corrupted file",
        ValidationCode.BackupFileInvalid,
        cause = cause,
    )

    // Insert-then-update rather than INSERT OR REPLACE: see the comment block in accounts.sq.
    // updatedAt is stamped with the import time on purpose — restoring a backup is an explicit
    // "make everything look like this file" action, so it must win LWW against the other devices.

    private fun restore(dto: AccountDto, now: Long) {
        db.accountsQueries.insertOrIgnoreFromBackup(
            accountId = dto.accountId,
            name = dto.name,
            type = dto.type,
            currency = dto.currency,
            updatedAt = now,
            createdAt = now,
        )
        db.accountsQueries.restoreFromBackup(
            name = dto.name,
            type = dto.type,
            currency = dto.currency,
            updatedAt = now,
            accountId = dto.accountId,
        )
    }

    /**
     * Restores one category, detaching first whatever the new type would strand.
     *
     * The parent half of the composite key, and the mirror of `CategoryTableSync.applyRemoteRow`.
     * `restoreFromBackup` writes `categoryType`, and changing a category's type while a movement
     * still holds the old pair is refused outright — not silently repaired, and not deferred:
     * the statement throws inside the ONE transaction that wraps the whole restore, so the import
     * rolls back after the tombstone sweep and reaches the user as a generic database error.
     *
     * `recurring_movements` is what makes it reachable rather than theoretical, and the sweep did
     * not remove the need — **a tombstone is not a delete.** `clearCategoryOnTypeChange` carries no
     * `deletedAt IS NULL` filter precisely because a row the file does not mention, freshly
     * tombstoned, still holds the pair it was created with and still makes SQLite refuse the
     * category's type change. Under a v1 or v2 file nothing was swept at all and every template on
     * the device is in that position. And the file is untrusted — a hand-edited `categoryType` is
     * all it takes, which is the same premise the movement restore below already works from.
     *
     * So this call is the one thing the import does to that table on EVERY version, gate or no gate.
     */
    private fun restore(dto: CategoryDto, now: Long) {
        db.transactionsQueries.clearCategoryOnTypeChange(
            categoryId = dto.categoryId,
            categoryType = dto.categoryType,
        )
        db.recurring_movementsQueries.clearCategoryOnTypeChange(
            categoryId = dto.categoryId,
            categoryType = dto.categoryType,
        )
        db.categoriesQueries.insertOrIgnoreFromBackup(
            categoryId = dto.categoryId,
            name = dto.name,
            icon = dto.icon,
            color = dto.color,
            categoryType = dto.categoryType,
            updatedAt = now,
            createdAt = now,
        )
        db.categoriesQueries.restoreFromBackup(
            name = dto.name,
            icon = dto.icon,
            color = dto.color,
            categoryType = dto.categoryType,
            updatedAt = now,
            categoryId = dto.categoryId,
        )
    }

    /**
     * Returns false when the row was NOT restored.
     *
     * The file is untrusted: it is plain JSON in storage the user chose, and it can have been
     * hand-edited, truncated or written by something else entirely. A row whose `occurredAt` is not
     * a local datetime is dropped here rather than written verbatim, because writing it is the
     * worst of the three options — the column is NOT NULL so the insert succeeds, `liveTotals` and
     * `getAccountBalance` add its amount to the Home balance, and every mapper that renders a
     * movement drops it. The balance would then disagree with the visible ledger, which is the
     * whole defect class this model exists to remove.
     *
     * The value is re-encoded from the parsed datetime rather than copied through, so the bytes in
     * the column are the canonical form no matter which shape the file used.
     */
    private fun restore(dto: TransactionDto, now: Long): Boolean {
        val transaction = dto.toEntityOrNull() ?: return false
        val occurredAt = transaction.occurredAt.toOccurredAtText()
        val type = transaction.type.name
        val categoryId = usableCategoryId(dto.categoryId, type)
        db.transactionsQueries.insertOrIgnoreFromBackup(
            transactionId = dto.transactionId,
            type = type,
            amount = dto.amountCents,
            description = dto.description,
            occurredAt = occurredAt,
            categoryId = categoryId,
            accountId = dto.accountId,
            createdAt = now,
            updatedAt = now,
        )
        db.transactionsQueries.restoreFromBackup(
            type = type,
            amount = dto.amountCents,
            description = dto.description,
            occurredAt = occurredAt,
            categoryId = categoryId,
            accountId = dto.accountId,
            updatedAt = now,
            transactionId = dto.transactionId,
        )
        return true
    }

    /**
     * Restores one template — the fourth table, and the only one an older file may not touch.
     *
     * Returns false when the row was NOT restored — the same signal `restore(dto: TransactionDto)`
     * uses, and for the same reason: it is what lets [ImportStats.recurring] count what LANDED
     * instead of the file's own length.
     *
     * A row the file carries but this build cannot read is dropped, not written: see
     * [RecurringMovementDto.toEntityOrNull], which also argues why an unparseable
     * `lastConfirmedPeriod` is repaired to null there instead of costing the whole template.
     *
     * `createdAt` comes off the FILE and never from [now], which is the opposite of every other
     * restore here and the single most destructive thing this function could get wrong: it floors
     * `RecurringDueRules.pendingPeriods`, so stamping the import's instant onto it swallows every
     * period the template still owed, with no error and nothing on screen.
     * `restoreFromBackup` writes it for the same reason — a re-import onto a device that still holds
     * the row has to take the file's value, not the one already in the column.
     */
    private fun restore(dto: RecurringMovementDto, now: Long): Boolean {
        val template = dto.toEntityOrNull() ?: return false
        val type = template.type.name
        val categoryId = usableCategoryId(dto.categoryId, type)
        db.recurring_movementsQueries.insertOrIgnoreFromBackup(
            id = template.id.value,
            name = template.name,
            type = type,
            amount = template.amount?.cents,
            description = template.description,
            categoryId = categoryId,
            accountId = template.accountId.value,
            frequency = template.frequency.name,
            dayOfMonth = template.dayOfMonth.toLong(),
            isActive = if (template.isActive) 1L else 0L,
            lastConfirmedPeriod = template.lastConfirmedPeriod,
            createdAt = template.createdAt,
            updatedAt = now,
        )
        db.recurring_movementsQueries.restoreFromBackup(
            name = template.name,
            type = type,
            amount = template.amount?.cents,
            description = template.description,
            categoryId = categoryId,
            accountId = template.accountId.value,
            frequency = template.frequency.name,
            dayOfMonth = template.dayOfMonth.toLong(),
            isActive = if (template.isActive) 1L else 0L,
            lastConfirmedPeriod = template.lastConfirmedPeriod,
            createdAt = template.createdAt,
            updatedAt = now,
            id = template.id.value,
        )
        return true
    }

    /**
     * The category id this row may actually be written with — [categoryId] itself, or null.
     *
     * Under the composite key a stale `(categoryId, type)` pair no longer inserts: the statement
     * aborts with a constraint violation inside the single transaction that wraps the whole restore,
     * so ONE bad row would roll back the entire import and leave the owner with nothing. That is the
     * one safety net on a device holding real accumulated data, so the row lands uncategorized
     * instead — the movement, its amount and its type are the data; the category is a label the user
     * can put back in two taps.
     *
     * **Both callers reach it by a different route, and only one of them is about legacy files.**
     *
     *  - Transactions: every backup file that exists today predates the composite key, and the export
     *    before it carried whatever pair the app had stored, mismatches included.
     *  - Templates: version 3 was born after the key existed, and the export's dangling-category
     *    scrub already keeps a tombstoned category's id out of the file — so a file **this app wrote**
     *    can never carry a broken recurring pair. The only provenance left is a hand-edited one.
     *
     * The guard stands either way: the file is untrusted input whatever wrote it, and a rollback
     * after the tombstone sweep has already run is the catastrophic outcome in both cases.
     *
     * Absent and mismatched are both handled the same way here, unlike in `sync/`, and the reason
     * is that the file is the whole world: every category the file carries is restored before the
     * first transaction and before the first template, inside this same transaction, so a category
     * still missing at this point is missing from the file, not late. There is no later arrival to
     * wait for — which is also why the templates are restored last rather than first.
     */
    private fun usableCategoryId(categoryId: String?, type: String): String? {
        if (categoryId == null) return null
        val storedType = db.categoriesQueries.typeOf(categoryId).executeAsOneOrNull()
        return if (storedType == type) categoryId else null
    }
}

/**
 * The four reads and the scrub between them, as one synchronous body.
 *
 * Called only from inside [DefaultBackupRepository.exportToJson]'s `transactionWithResult`, and
 * top-level rather than a member of that class for a mechanical reason: the class already sits
 * exactly on detekt's `allowedFunctionsPerClass: 11`. It needs nothing but the database.
 */
private fun EmmDatabaseData.snapshot(exportedAt: Long, appVersion: String): ExportPayloadDto {
    val exportedCategories = categoriesQueries.all().executeAsList()
        .asEntity().asExternalModel().map { it.toDto() }
    val liveCategoryIds = exportedCategories.mapTo(mutableSetOf()) { it.categoryId }

    // Deleting a category tombstones it without touching the movements filed under it, so a
    // live transaction can still carry the id of a category that is not exported. Keeping that
    // id would produce a backup whose own import fails on the categoryId foreign key, so the
    // dangling reference is dropped here — the movement restores as uncategorized, which is
    // exactly how it already reads on screen.
    //
    // The scrub itself is unchanged. What changed is that `liveCategoryIds` and the rows below now
    // come out of the SAME snapshot, so a category deleted mid-export can no longer be live for one
    // of the two reads and gone for the other.
    val exportedTransactions = transactionsQueries.all().executeAsList()
        .asEntity().asExternalModel().map { transaction ->
            val dto = transaction.toDto()
            if (dto.categoryId != null && dto.categoryId !in liveCategoryIds) dto.copy(categoryId = null) else dto
        }

    // The same scrub, for the same reason: `recurring_movements` carries the identical composite
    // key (categoryId, type) -> categories(categoryId, categoryType), so a template pointing at a
    // tombstoned category writes a file that fails its own import.
    val exportedRecurring = recurring_movementsQueries.selectAllLive().executeAsList()
        .asEntity().asExternalModel().map { template ->
            val dto = template.toDto()
            if (dto.categoryId != null && dto.categoryId !in liveCategoryIds) dto.copy(categoryId = null) else dto
        }

    return ExportPayloadDto(
        exportedAt = exportedAt,
        appVersion = appVersion,
        accounts = accountsQueries.all().executeAsList().asEntity().asExternalModel().map { it.toDto() },
        categories = exportedCategories,
        transactions = exportedTransactions,
        recurringMovements = exportedRecurring,
    )
}
