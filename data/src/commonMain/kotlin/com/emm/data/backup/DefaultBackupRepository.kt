package com.emm.data.backup

import com.emm.data.EmmDatabaseData
import com.emm.data.account.asEntity
import com.emm.data.account.asExternalModel
import com.emm.data.category.asEntity
import com.emm.data.category.asExternalModel
import com.emm.data.loan.asEntity
import com.emm.data.loan.asExternalModel
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

private val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

class DefaultBackupRepository(private val db: EmmDatabaseData, private val clock: Clock) : BackupRepository {

    override suspend fun exportToJson(exportedAt: Long, appVersion: String): String {
        val payload = safeDbCall {
            withContext(ioDispatcher) {
                db.transactionWithResult { db.snapshot(exportedAt, appVersion) }
            }
        }
        return encodeAsDomainException { exportJson.encodeToString(payload) }
    }

    override suspend fun importFromJson(json: String): ImportStats {
        val decoded = decodeBackupPayload(json)
        val payload = decoded.payload
        val fileCarriesRecurring = decoded.declaredVersion >= BACKUP_RECURRING_SINCE_VERSION
        val fileCarriesLoans = decoded.declaredVersion >= BACKUP_LOANS_SINCE_VERSION

        return safeDbCall {
            val now = clock.nowMillis()
            var restoredTransactions = 0
            var restoredRecurring = 0
            var restoredLoans = 0
            var restoredLoanPayments = 0
            db.transaction {
                db.transactionsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                if (fileCarriesRecurring) {
                    db.recurring_movementsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                }
                if (fileCarriesLoans) {
                    db.loan_paymentsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                    db.loansQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                }
                db.categoriesQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
                db.accountsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)

                payload.accounts.forEach { dto -> restore(dto, now) }
                payload.categories.forEach { dto -> restore(dto, now) }
                restoredTransactions = payload.transactions.count { dto -> restore(dto, now) }
                if (fileCarriesRecurring) {
                    restoredRecurring = payload.recurringMovements.count { dto -> restore(dto, now) }
                }
                if (fileCarriesLoans) {
                    restoredLoans = payload.loans.count { dto -> restore(dto, now) }
                    restoredLoanPayments = payload.loanPayments.count { dto -> restore(dto, now) }
                }
            }

            ImportStats(
                accounts = payload.accounts.size,
                categories = payload.categories.size,
                transactions = restoredTransactions,
                recurring = restoredRecurring,
                loans = restoredLoans,
                loanPayments = restoredLoanPayments,
            )
        }
    }

    override suspend fun latestLocalChangeAt(): Long? = safeDbCall {
        withContext(ioDispatcher) {
            db.backupQueries.latestLocalChange().executeAsOne().updatedAt
        }
    }

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

    private fun restore(dto: TransactionDto, now: Long): Boolean {
        val transaction = dto.toEntityOrNull() ?: return false
        val occurredAt = transaction.occurredAt.toOccurredAtText()
        val type = transaction.type.name
        val categoryId = db.usableCategoryId(dto.categoryId, type)
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

    private fun restore(dto: RecurringMovementDto, now: Long): Boolean {
        val template = dto.toEntityOrNull() ?: return false
        val type = template.type.name
        val categoryId = db.usableCategoryId(dto.categoryId, type)
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

    private fun restore(dto: LoanDto, now: Long): Boolean {
        val loan = dto.toEntityOrNull() ?: return false
        val lentAt = loan.lentAt.toOccurredAtText()
        db.loansQueries.insertOrIgnoreFromBackup(
            loanId = loan.id.value,
            personName = loan.personName,
            personKey = loan.personKey,
            principal = loan.principal.cents,
            interestBps = loan.interestBps.toLong(),
            totalDue = loan.totalDue.cents,
            note = loan.note,
            lentAt = lentAt,
            createdAt = now,
            updatedAt = now,
        )
        db.loansQueries.restoreFromBackup(
            personName = loan.personName,
            personKey = loan.personKey,
            principal = loan.principal.cents,
            interestBps = loan.interestBps.toLong(),
            totalDue = loan.totalDue.cents,
            note = loan.note,
            lentAt = lentAt,
            updatedAt = now,
            loanId = loan.id.value,
        )
        return true
    }

    private fun restore(dto: LoanPaymentDto, now: Long): Boolean {
        val payment = dto.toEntityOrNull()?.takeIf { db.holdsLiveLoan(it.loanId.value) } ?: return false
        val paidAt = payment.paidAt.toOccurredAtText()
        db.loan_paymentsQueries.insertOrIgnoreFromBackup(
            paymentId = payment.id.value,
            loanId = payment.loanId.value,
            amount = payment.amount.cents,
            method = payment.method.name,
            paidAt = paidAt,
            note = payment.note,
            createdAt = now,
            updatedAt = now,
        )
        db.loan_paymentsQueries.restoreFromBackup(
            loanId = payment.loanId.value,
            amount = payment.amount.cents,
            method = payment.method.name,
            paidAt = paidAt,
            note = payment.note,
            updatedAt = now,
            paymentId = payment.id.value,
        )
        return true
    }
}

internal inline fun <T> encodeAsDomainException(block: () -> T): T = try {
    block()
} catch (e: SerializationException) {
    throw DomainException.SerializationError(e)
}

// `loan_payments.loanId` is NOT NULL, so a payment whose loan is absent cannot be detached the way
// a transaction's category is — inserting it would abort the whole restore on the foreign key.
private fun EmmDatabaseData.holdsLiveLoan(loanId: String): Boolean =
    loansQueries.byId(loanId).executeAsOneOrNull() != null

private fun EmmDatabaseData.usableCategoryId(categoryId: String?, type: String): String? {
    if (categoryId == null) return null
    val storedType = categoriesQueries.typeOf(categoryId).executeAsOneOrNull()
    return if (storedType == type) categoryId else null
}

private fun EmmDatabaseData.snapshot(exportedAt: Long, appVersion: String): ExportPayloadDto {
    val exportedCategories = categoriesQueries.all().executeAsList()
        .asEntity().asExternalModel().map { it.toDto() }
    val liveCategoryIds = exportedCategories.mapTo(mutableSetOf()) { it.categoryId }

    val exportedTransactions = transactionsQueries.all().executeAsList()
        .asEntity().asExternalModel().map { transaction ->
            val dto = transaction.toDto()
            if (dto.categoryId != null && dto.categoryId !in liveCategoryIds) dto.copy(categoryId = null) else dto
        }

    val exportedRecurring = recurring_movementsQueries.selectAllLive().executeAsList()
        .asEntity().asExternalModel().map { template ->
            val dto = template.toDto()
            if (dto.categoryId != null && dto.categoryId !in liveCategoryIds) dto.copy(categoryId = null) else dto
        }

    val exportedLoans = loansQueries.all().executeAsList()
        .asEntity().asExternalModel().map { it.toDto() }
    val liveLoanIds = exportedLoans.mapTo(mutableSetOf()) { it.loanId }

    val exportedLoanPayments = loan_paymentsQueries.all().executeAsList()
        .asEntity().asExternalModel().map { it.toDto() }
        .filter { it.loanId in liveLoanIds }

    return ExportPayloadDto(
        exportedAt = exportedAt,
        appVersion = appVersion,
        accounts = accountsQueries.all().executeAsList().asEntity().asExternalModel().map { it.toDto() },
        categories = exportedCategories,
        transactions = exportedTransactions,
        recurringMovements = exportedRecurring,
        loans = exportedLoans,
        loanPayments = exportedLoanPayments,
    )
}
