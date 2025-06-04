package com.emm.data.transaction

import com.emm.data.Transactions
import com.emm.data.sync.Synchronizer
import com.emm.domain.account.Account
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionUpdate
import kotlinx.coroutines.flow.Flow

class DefaultTransactionRepository(
    private val localDataSource: TransactionLocalDataSource,
    private val remoteDataSource: TransactionRemoteDataSource,
) : TransactionRepository, Synchronizer {

    override suspend fun create(transactionInsert: TransactionInsert) {
        return localDataSource.create(transactionInsert)
    }

    override fun retrieve(): Flow<List<Transaction>> {
        return localDataSource.retrieve()
    }

    override fun sumIncome(accountId: String): Flow<Double> {
        return localDataSource.sumIncome(accountId)
    }

    override fun sumSpend(accountId: String): Flow<Double> {
        return localDataSource.sumSpend(accountId)
    }

    override fun difference(accountId: String): Flow<Double> {
        return localDataSource.difference(accountId)
    }

    override suspend fun delete(transactionId: String) {
        localDataSource.delete(transactionId)
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
        val transactionUpdates: List<TransactionUpdate> = unSyncedTransactions.map(::transactionUpdate)
        unSyncedTransactions.zip(transactionUpdates) { transaction, update ->
            localDataSource.update(transaction.transactionId, update)
        }
    }

    private fun transactionUpdate(transaction: Transactions): TransactionUpdate {
        val wrapperAccount = Account(
            accountId = transaction.accountId,
            name = "",
            balance = 0.0,
        )
        return TransactionUpdate(
            type = TransactionType.valueOf(transaction.type),
            amount = transaction.amount,
            description = transaction.description,
            account = wrapperAccount,
            date = transaction.date,
            isSynced = true,
        )
    }

    private suspend fun updateRemote(unSyncedTransactions: List<Transactions>) {
        val transactionModels: List<TransactionModel> = unSyncedTransactions.map(Transactions::toModel)
        remoteDataSource.upsert(transactionModels)
    }
}