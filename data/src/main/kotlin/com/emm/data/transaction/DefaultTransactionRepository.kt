package com.emm.data.transaction

import com.emm.data.Transactions
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionUpdate
import kotlinx.coroutines.flow.Flow

class DefaultTransactionRepository(
    private val localDataSource: TransactionLocalDataSource,
    private val remoteDataSource: TransactionRemoteDataSource,
) : TransactionRepository {

    override suspend fun create(transactionInsert: TransactionInsert) {
        return localDataSource.create(transactionInsert)
    }

    override fun all(): Flow<List<Transaction>> {
        return localDataSource.all()
    }

    override suspend fun delete(transactionId: String) {
        localDataSource.delete(transactionId)
    }

    override suspend fun pull() {
        val transactionModels: List<TransactionModel> = remoteDataSource.all()
        val transactionUpdates: List<TransactionInsert> = transactionModels.map(TransactionModel::toTransactionInsert)
        transactionUpdates.forEach { transactionInsert ->
            localDataSource.create(transactionInsert)
        }
    }

    override fun find(transactionId: String): Transaction? {
        return localDataSource.find(transactionId)
    }

    override suspend fun sync() {
        val unSyncedTransactions: List<Transactions> = localDataSource.unSynced()
        updateRemote(unSyncedTransactions)
        updateLocal(unSyncedTransactions)
    }

    private suspend fun updateLocal(unSyncedTransactions: List<Transactions>) {
        val transactionUpdates: List<TransactionUpdate> = unSyncedTransactions.map(Transactions::toTransactionUpdate)
        unSyncedTransactions.zip(transactionUpdates) { transaction, update ->
            localDataSource.update(transaction.transactionId, update)
        }
    }

    private suspend fun updateRemote(unSyncedTransactions: List<Transactions>) {
        val transactionModels: List<TransactionModel> = unSyncedTransactions.map(Transactions::toModel)
        remoteDataSource.upsert(transactionModels)
    }
}