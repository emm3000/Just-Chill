package com.emm.data.backup

import com.emm.data.EmmDatabaseData
import com.emm.data.shared.safeDbCall
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
import kotlin.time.Clock

private val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

private val importJson = Json {
    ignoreUnknownKeys = true
}

class DefaultBackupRepository(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val accounts: AccountRepository,
    private val db: EmmDatabaseData,
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
        val payload = try {
            importJson.decodeFromString<ExportPayloadDto>(json)
        } catch (e: SerializationException) {
            throw DomainException.ValidationError(
                "Invalid or corrupted file",
                ValidationCode.BackupFileInvalid,
                cause = e,
            )
        } catch (e: IllegalArgumentException) {
            // Enum value not found when deserializing DTOs
            throw DomainException.ValidationError(
                "Invalid or corrupted file",
                ValidationCode.BackupFileInvalid,
                cause = e,
            )
        }

        if (payload.schemaVersion != BACKUP_SCHEMA_VERSION) {
            throw DomainException.ValidationError(
                "Unsupported file version.",
                ValidationCode.BackupVersionUnsupported,
            )
        }

        return safeDbCall {
            val now = Clock.System.now().toEpochMilliseconds()
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
                payload.transactions.forEach { dto -> restore(dto, now) }
            }

            ImportStats(
                accounts = payload.accounts.size,
                categories = payload.categories.size,
                transactions = payload.transactions.size,
            )
        }
    }

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

    private fun restore(dto: TransactionDto, now: Long) {
        db.transactionsQueries.insertOrIgnoreFromBackup(
            transactionId = dto.transactionId,
            type = dto.type,
            amount = dto.amountCents,
            description = dto.description,
            occurredAt = dto.occurredAt,
            categoryId = dto.categoryId,
            accountId = dto.accountId,
            createdAt = now,
            updatedAt = now,
        )
        db.transactionsQueries.restoreFromBackup(
            type = dto.type,
            amount = dto.amountCents,
            description = dto.description,
            occurredAt = dto.occurredAt,
            categoryId = dto.categoryId,
            accountId = dto.accountId,
            updatedAt = now,
            transactionId = dto.transactionId,
        )
    }
}
