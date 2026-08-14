package com.emm.data.backup

import com.emm.data.EmmDatabaseData
import com.emm.data.shared.nowMillis
import com.emm.data.shared.safeDbCall
import com.emm.data.shared.toOccurredAtText
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.ImportStats
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.first
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

class DefaultBackupRepository(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val accounts: AccountRepository,
    /**
     * Read through the domain interface like the other three, and NOT through [db].
     *
     * A direct query for this one table would leave the export mockable for three tables and not the
     * fourth, which is the property `DefaultBackupRepositoryTest` is built on. `allLive()` exists on
     * that interface for this caller: `allActive()` would silently drop every paused template.
     */
    private val recurring: RecurringMovementRepository,
    private val db: EmmDatabaseData,
    private val clock: Clock,
) : BackupRepository {

    override suspend fun exportToJson(exportedAt: Long, appVersion: String): String {
        val exportedCategories = categories.all().first().map { it.toDto() }
        val liveCategoryIds = exportedCategories.mapTo(mutableSetOf()) { it.categoryId }

        // Deleting a category tombstones it without touching the movements filed under it, so a
        // live transaction can still carry the id of a category that is not exported. Keeping that
        // id would produce a backup whose own import fails on the categoryId foreign key, so the
        // dangling reference is dropped here — the movement restores as uncategorized, which is
        // exactly how it already reads on screen.
        val exportedTransactions = transactions.all().first().map { transaction ->
            val dto = transaction.toDto()
            if (dto.categoryId != null && dto.categoryId !in liveCategoryIds) dto.copy(categoryId = null) else dto
        }

        // The same scrub, for the same reason: `recurring_movements` carries the identical composite
        // key (categoryId, type) -> categories(categoryId, categoryType), so a template pointing at a
        // tombstoned category writes a file that fails its own import.
        val exportedRecurring = recurring.allLive().first().map { template ->
            val dto = template.toDto()
            if (dto.categoryId != null && dto.categoryId !in liveCategoryIds) dto.copy(categoryId = null) else dto
        }

        val payload = ExportPayloadDto(
            exportedAt = exportedAt,
            appVersion = appVersion,
            accounts = accounts.all().first().map { it.toDto() },
            categories = exportedCategories,
            transactions = exportedTransactions,
            recurringMovements = exportedRecurring,
        )
        return exportJson.encodeToString(payload)
    }

    override suspend fun importFromJson(json: String): ImportStats {
        // Both halves of the decode, deliberately: `payload` is the current shape, and
        // `decoded.declaredVersion` is the version the FILE declared — the only thing that can tell
        // a v1/v2 file apart from a v3 one exported by a device with no templates. Nothing reads it
        // yet; the version-gated sweep is what will. See [DecodedBackup].
        val decoded = decodePayload(json)
        val payload = decoded.payload

        return safeDbCall {
            // One read, reused by the tombstone sweep and by every row restored after it — the
            // discipline the rest of :data already follows. It used to be `Clock.System.now()`,
            // which made this the only writer in the module a test could not pin, on the one path
            // that rewrites every row the user owns at once.
            val now = clock.nowMillis()
            var restoredTransactions = 0
            db.transaction {
                // "Replace everything" expressed as tombstones instead of physical deletes.
                // Every live row is tombstoned first, then the backup brings back exactly what it
                // carries. Rows the file does not mention keep their tombstone, so the removal
                // pushes to the server and reaches the other devices — a physical DELETE left no
                // trace, and the next pull simply downloaded the rows again.
                //
                // Recurring movements are untouched here — not swept, not restored — even though
                // the format now carries them. Adding the field and acting on it are two commits on
                // purpose: the destructive half has to be version-gated, since a v1/v2 file has no
                // templates to give and wiping on one would destroy data no backup can put back.
                // Until that lands, a v3 file carries templates an import does not restore, and
                // BackupV3CompatibilityTest pins that the rows already on the device survive it.
                db.transactionsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                db.categoriesQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                db.accountsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)

                // ORDER MATTERS: a restored transaction references its account and category.
                payload.accounts.forEach { dto -> restore(dto, now) }
                payload.categories.forEach { dto -> restore(dto, now) }
                restoredTransactions = payload.transactions.count { dto -> restore(dto, now) }
            }

            // The count is what LANDED, not what the file held. Reporting the file's own length
            // would say "3 movimientos importados" while one of them is nowhere on screen — and
            // the balance would still include its amount, because the totals aggregate reads the
            // column and the mappers that hide the row never run on it.
            ImportStats(
                accounts = payload.accounts.size,
                categories = payload.categories.size,
                transactions = restoredTransactions,
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
     * `recurring_movements` is what makes it reachable rather than theoretical: the import
     * deliberately never touches that table, so its rows still hold whatever pair they were
     * created with while the file rewrites the category out from under them. And the file is
     * untrusted — a hand-edited `categoryType` is all it takes, which is the same premise the
     * movement restore below already works from.
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
     * The category id this row may actually be written with — [categoryId] itself, or null.
     *
     * **Every backup file that exists today predates the composite key**, and the export before it
     * carried whatever pair the app had stored, mismatches included. Under the key those rows no
     * longer insert: the statement aborts with a constraint violation inside the single
     * transaction that wraps the whole restore, so ONE stale row would roll back the entire import
     * and leave the owner with nothing. That is the one safety net on a device holding real
     * accumulated data, so the row lands uncategorized instead — the movement, its amount and its
     * type are the data; the category is a label the user can put back in two taps.
     *
     * Absent and mismatched are both handled the same way here, unlike in `sync/`, and the reason
     * is that the file is the whole world: categories are restored before transactions inside this
     * same transaction, so a category still missing at this point is missing from the file, not
     * late. There is no later arrival to wait for.
     */
    private fun usableCategoryId(categoryId: String?, type: String): String? {
        if (categoryId == null) return null
        val storedType = db.categoriesQueries.typeOf(categoryId).executeAsOneOrNull()
        return if (storedType == type) categoryId else null
    }
}
