package com.emm.data.transaction

import com.emm.data.Transactions
import com.emm.data.account.AccountRemoteDataSource
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class DefaultTransactionRepository(
    private val localDataSource: TransactionLocalDataSource,
    private val remoteDataSource: TransactionRemoteDataSource,
    private val accountRemoteDataSource: AccountRemoteDataSource,
) : TransactionRepository {

    override suspend fun create(transactionInsert: TransactionInsert) {
        return localDataSource.create(transactionInsert)
    }

    override fun all(): Flow<List<Transaction>> {
        return localDataSource.all()
    }

    override suspend fun delete(transactionId: String) {
        localDataSource.softDelete(transactionId)
    }

    override suspend fun pull() {
        val accountIds: List<String> = accountRemoteDataSource.all().map { it.accountId }
        val transactionModels: List<TransactionModel> = remoteDataSource.all(accountIds)
        val transactionUpdates: List<TransactionInsert> = transactionModels.map(TransactionModel::toTransactionInsert)
        transactionUpdates.forEach { transactionInsert ->
            localDataSource.create(transactionInsert)
        }
    }

    override fun find(transactionId: String): Transaction? {
        return localDataSource.find(transactionId)
    }

    override suspend fun sync() {
        val deletionsAndUpdates: Pair<List<Transactions>, List<Transactions>> = localDataSource
            .unSynced()
            .partition(Transactions::isDeleted)

        val deletedTransactionIds: List<String> = deletionsAndUpdates.first.map(Transactions::transactionId)
        remoteDataSource.deleteMultipleRows(deletedTransactionIds)
        deletedTransactionIds.forEach { localDataSource.hardDelete(it) }

        updateRemote(deletionsAndUpdates.second)
        updateLocal(deletionsAndUpdates.second)
    }

    private suspend fun updateLocal(unSyncedTransactions: List<Transactions>) {
        unSyncedTransactions.forEach { localDataSource.markAsSynced(it.transactionId) }
    }

    private suspend fun updateRemote(unSyncedTransactions: List<Transactions>) {
        val transactionModels: List<TransactionModel> = unSyncedTransactions.map(Transactions::toModel)
        remoteDataSource.upsert(transactionModels)
    }
}