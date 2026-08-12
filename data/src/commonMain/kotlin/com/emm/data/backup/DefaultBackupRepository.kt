package com.emm.data.backup

import com.emm.data.EmmDatabaseData
import com.emm.data.shared.nowMillis
import com.emm.data.shared.safeDbCall
import com.emm.data.shared.toOccurredAtText
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
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

        val payload = ExportPayloadDto(
            exportedAt = exportedAt,
            appVersion = appVersion,
            accounts = accounts.all().first().map { it.toDto() },
            categories = exportedCategories,
            transactions = exportedTransactions,
        )
        return exportJson.encodeToString(payload)
    }

    override suspend fun importFromJson(json: String): ImportStats {
        val payload = decodePayload(json)

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
                // Recurring movements are deliberately untouched: the export format does not
                // include them, so wiping them would destroy data that no backup can restore.
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
     * Reads the file at whichever format version it declares.
     *
     * The version is read FIRST, from the raw JSON, and only then is the payload decoded as the
     * shape that version actually had. Decoding optimistically and falling back would mean a v1
     * file was parsed by v2's rules first, which is exactly the mistake that makes an old file
     * report itself as corrupt.
     */
    private fun decodePayload(json: String): ExportPayloadDto {
        val root = parse { importJson.parseToJsonElement(json).jsonObject }

        return when (schemaVersionOf(root)) {
            BACKUP_SCHEMA_VERSION -> parse {
                importJson.decodeFromJsonElement<ExportPayloadDto>(root)
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

    private fun restore(dto: CategoryDto, now: Long) {
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
        db.transactionsQueries.insertOrIgnoreFromBackup(
            transactionId = dto.transactionId,
            type = transaction.type.name,
            amount = dto.amountCents,
            description = dto.description,
            occurredAt = occurredAt,
            categoryId = dto.categoryId,
            accountId = dto.accountId,
            createdAt = now,
            updatedAt = now,
        )
        db.transactionsQueries.restoreFromBackup(
            type = transaction.type.name,
            amount = dto.amountCents,
            description = dto.description,
            occurredAt = occurredAt,
            categoryId = dto.categoryId,
            accountId = dto.accountId,
            updatedAt = now,
            transactionId = dto.transactionId,
        )
        return true
    }
}
