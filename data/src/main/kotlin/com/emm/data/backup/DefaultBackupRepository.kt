package com.emm.data.backup

import com.emm.data.EmmDatabaseData
import com.emm.data.shared.safeDbCall
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.ImportStats
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        val payload = ExportPayloadDto(
            exportedAt = exportedAt,
            appVersion = appVersion,
            accounts = accounts.all().first().map { it.toDto() },
            categories = categories.all().first().map { it.toDto() },
            transactions = transactions.all().first().map { it.toDto() },
        )
        return exportJson.encodeToString(payload)
    }

    override suspend fun importFromJson(json: String): ImportStats {
        val payload = try {
            importJson.decodeFromString<ExportPayloadDto>(json)
        } catch (e: SerializationException) {
            throw DomainException.ValidationError("Archivo no válido o corrupto", cause = e)
        } catch (e: IllegalArgumentException) {
            // Enum value not found when deserializing DTOs
            throw DomainException.ValidationError("Archivo no válido o corrupto", cause = e)
        }

        if (payload.schemaVersion != 1) {
            throw DomainException.ValidationError("Versión de archivo no soportada.")
        }

        return safeDbCall {
            val now = System.currentTimeMillis()
            db.transaction {
                // ORDER MATTERS: transactions reference accounts + categories via FK.
                // Delete child tables first, then parents, to satisfy ON DELETE RESTRICT.
                db.transactionsQueries.deleteAll()
                db.categoriesQueries.deleteAll()
                db.accountsQueries.deleteAll()

                payload.accounts.forEach { dto ->
                    db.accountsQueries.insert(
                        accountId = dto.accountId,
                        name = dto.name,
                        type = dto.type,
                        currency = dto.currency,
                        updatedAt = now,
                        createdAt = now,
                    )
                }

                payload.categories.forEach { dto ->
                    db.categoriesQueries.insert(
                        categoryId = dto.categoryId,
                        name = dto.name,
                        icon = dto.icon,
                        color = dto.color,
                        categoryType = dto.categoryType,
                        isDefault = false,
                        updatedAt = now,
                        createdAt = now,
                    )
                }

                payload.transactions.forEach { dto ->
                    db.transactionsQueries.insert(
                        transactionId = dto.transactionId,
                        type = dto.type,
                        amount = dto.amountCents,
                        description = dto.description,
                        date = dto.date,
                        categoryId = dto.categoryId,
                        accountId = dto.accountId,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
            }

            ImportStats(
                accounts = payload.accounts.size,
                categories = payload.categories.size,
                transactions = payload.transactions.size,
            )
        }
    }
}
