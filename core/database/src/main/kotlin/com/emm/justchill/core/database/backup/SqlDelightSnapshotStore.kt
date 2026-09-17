package com.emm.justchill.core.database.backup

import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.database.account.asEntity
import com.emm.justchill.core.database.account.asExternalModel
import com.emm.justchill.core.database.category.asEntity
import com.emm.justchill.core.database.category.asExternalModel
import com.emm.justchill.core.database.loan.asEntity
import com.emm.justchill.core.database.loan.asExternalModel
import com.emm.justchill.core.database.recurring.asEntity
import com.emm.justchill.core.database.recurring.asExternalModel
import com.emm.justchill.core.database.shared.ioDispatcher
import com.emm.justchill.core.database.shared.nowMillis
import com.emm.justchill.core.database.shared.safeDbCall
import com.emm.justchill.core.database.transaction.asEntity
import com.emm.justchill.core.database.transaction.asExternalModel
import com.emm.justchill.core.domain.shared.backup.ImportStats
import com.emm.justchill.core.domain.shared.backup.LocalSnapshot
import com.emm.justchill.core.domain.shared.backup.SnapshotStore
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class SqlDelightSnapshotStore(private val db: JustChillDatabase, private val clock: Clock) : SnapshotStore {

    override suspend fun export(): LocalSnapshot = safeDbCall {
        withContext(ioDispatcher) {
            db.transactionWithResult { db.readLive() }
        }
    }

    override suspend fun restore(snapshot: LocalSnapshot): ImportStats = safeDbCall {
        withContext(ioDispatcher) {
            db.replaceWith(snapshot, clock.nowMillis())
        }
    }

    override suspend fun latestLocalChangeAt(): Long? = safeDbCall {
        withContext(ioDispatcher) {
            db.backupQueries.latestLocalChange().executeAsOne().updatedAt
        }
    }
}

private fun JustChillDatabase.readLive(): LocalSnapshot = LocalSnapshot(
    accounts = accountsQueries.all().executeAsList().asEntity().asExternalModel(),
    categories = categoriesQueries.all().executeAsList().asEntity().asExternalModel(),
    transactions = transactionsQueries.all().executeAsList().asEntity().asExternalModel(),
    recurringMovements = recurring_movementsQueries.selectAllLive().executeAsList().asEntity().asExternalModel(),
    loans = loansQueries.all().executeAsList().asEntity().asExternalModel(),
    loanPayments = loan_paymentsQueries.all().executeAsList().asEntity().asExternalModel(),
)

private fun JustChillDatabase.replaceWith(snapshot: LocalSnapshot, now: Long): ImportStats {
    var restoredLoanPayments = 0
    transaction {
        transactionsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
        if (snapshot.recurringMovements != null) {
            recurring_movementsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
        }
        if (snapshot.loanPayments != null) {
            loan_paymentsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
        }
        if (snapshot.loans != null) {
            loansQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
        }
        categoriesQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)
        accountsQueries.softDeleteAllLive(deletedAt = now, updatedAt = now)

        snapshot.accounts.forEach { account -> restore(account, now) }
        snapshot.categories.forEach { category -> restore(category, now) }
        snapshot.transactions.forEach { transaction -> restore(transaction, now) }
        snapshot.recurringMovements?.forEach { template -> restore(template, now) }
        snapshot.loans?.forEach { loan -> restore(loan, now) }
        restoredLoanPayments = snapshot.loanPayments?.count { payment -> restore(payment, now) } ?: 0
    }

    return ImportStats(
        accounts = snapshot.accounts.size,
        categories = snapshot.categories.size,
        transactions = snapshot.transactions.size,
        recurring = snapshot.recurringMovements?.size ?: 0,
        loans = snapshot.loans?.size ?: 0,
        loanPayments = restoredLoanPayments,
    )
}
