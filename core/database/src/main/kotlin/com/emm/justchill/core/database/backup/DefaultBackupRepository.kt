package com.emm.justchill.core.database.backup

import com.emm.justchill.core.database.shared.ioDispatcher
import com.emm.justchill.core.domain.shared.backup.BackupRepository
import com.emm.justchill.core.domain.shared.backup.ImportStats
import com.emm.justchill.core.domain.shared.backup.LocalSnapshot
import com.emm.justchill.core.domain.shared.backup.SnapshotStore
import com.emm.justchill.core.domain.shared.error.DomainException
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

class DefaultBackupRepository(private val store: SnapshotStore) : BackupRepository {

    override suspend fun exportToJson(exportedAt: Long, appVersion: String): String {
        val payload: ExportPayloadDto = store.export().toPayload(exportedAt, appVersion)
        return encodeAsDomainException { exportJson.encodeToString(payload) }
    }

    override suspend fun importFromJson(json: String): ImportStats {
        val snapshot: LocalSnapshot = withContext(ioDispatcher) { decodeBackupPayload(json).toLocalSnapshot() }
        return store.restore(snapshot)
    }

    override suspend fun latestLocalChangeAt(): Long? = store.latestLocalChangeAt()
}

private fun LocalSnapshot.toPayload(exportedAt: Long, appVersion: String): ExportPayloadDto {
    val exportedCategories: List<CategoryDto> = categories.map { category -> category.toDto() }
    val liveCategoryIds: Set<String> = exportedCategories.mapTo(mutableSetOf()) { it.categoryId }

    val exportedTransactions: List<TransactionDto> = transactions.map { transaction ->
        val dto: TransactionDto = transaction.toDto()
        if (dto.categoryId != null && dto.categoryId !in liveCategoryIds) dto.copy(categoryId = null) else dto
    }

    val exportedRecurring: List<RecurringMovementDto> = recurringMovements.orEmpty().map { template ->
        val dto: RecurringMovementDto = template.toDto()
        if (dto.categoryId != null && dto.categoryId !in liveCategoryIds) dto.copy(categoryId = null) else dto
    }

    val exportedLoans: List<LoanDto> = loans.orEmpty().map { loan -> loan.toDto() }
    val liveLoanIds: Set<String> = exportedLoans.mapTo(mutableSetOf()) { it.loanId }

    return ExportPayloadDto(
        exportedAt = exportedAt,
        appVersion = appVersion,
        accounts = accounts.map { account -> account.toDto() },
        categories = exportedCategories,
        transactions = exportedTransactions,
        recurringMovements = exportedRecurring,
        loans = exportedLoans,
        loanPayments = loanPayments.orEmpty().map { payment -> payment.toDto() }
            .filter { it.loanId in liveLoanIds },
    )
}

private fun DecodedBackup.toLocalSnapshot(): LocalSnapshot {
    val carriesRecurring: Boolean = declaredVersion >= BACKUP_RECURRING_SINCE_VERSION
    val carriesLoans: Boolean = declaredVersion >= BACKUP_LOANS_SINCE_VERSION
    return LocalSnapshot(
        accounts = payload.accounts.map(AccountDto::toEntity),
        categories = payload.categories.mapNotNull(CategoryDto::toEntityOrNull),
        transactions = payload.transactions.mapNotNull(TransactionDto::toEntityOrNull),
        recurringMovements = payload.recurringMovements.mapNotNull(RecurringMovementDto::toEntityOrNull)
            .takeIf { carriesRecurring },
        loans = payload.loans.mapNotNull(LoanDto::toEntityOrNull).takeIf { carriesLoans },
        loanPayments = payload.loanPayments.mapNotNull(LoanPaymentDto::toEntityOrNull).takeIf { carriesLoans },
    )
}

internal inline fun <T> encodeAsDomainException(block: () -> T): T = try {
    block()
} catch (e: SerializationException) {
    throw DomainException.SerializationError(e)
}
