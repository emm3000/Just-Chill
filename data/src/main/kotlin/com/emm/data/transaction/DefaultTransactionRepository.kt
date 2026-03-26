package com.emm.data.transaction

import com.emm.data.account.AccountRemoteDataSource
import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeApiCall
import com.emm.data.shared.safeDbCall
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

class DefaultTransactionRepository(
    private val localDataSource: TransactionLocalDataSource,
    private val remoteDataSource: TransactionRemoteDataSource,
    private val accountRemoteDataSource: AccountRemoteDataSource,
) : TransactionRepository {

    override suspend fun create(transactionInsert: TransactionInsert) = safeDbCall {
        localDataSource.create(transactionInsert)
    }

    override fun all(): Flow<List<Transaction>> {
        return localDataSource.all().catchAsDomainException()
    }

    override fun fetchAllWithCategory(): Flow<List<TransactionWithCategory>> {
        return localDataSource.completeTransactions().catchAsDomainException()
    }

    override suspend fun delete(transactionId: String): Unit = safeDbCall {
        localDataSource.softDelete(transactionId)
        Unit
    }

    override suspend fun pull() = safeApiCall {
        val accountIds: List<String> = accountRemoteDataSource.all().map { it.accountId }
        val networkTransactions: List<NetworkTransaction> = remoteDataSource.all(accountIds)
        val transactionInserts: List<TransactionInsert> = networkTransactions.map { network ->
            network.asEntity().let { entity ->
                TransactionInsert(
                    id = entity.transactionId,
                    type = enumValueOf(entity.type),
                    amount = entity.amount,
                    description = entity.description,
                    date = entity.date,
                    accountId = entity.accountId,
                    updatedAt = entity.updatedAt,
                    createdAt = entity.createdAt,
                    categoryId = entity.categoryId,
                )
            }
        }
        transactionInserts.forEach { transactionInsert ->
            localDataSource.create(transactionInsert)
        }
    }

    override fun find(transactionId: String): Transaction? {
        return localDataSource.find(transactionId)
    }

    override suspend fun sync() = safeApiCall {
        val unSynced: List<TransactionEntity> = localDataSource.unSynced()

        val deletionsAndUpdates: Pair<List<TransactionEntity>, List<TransactionEntity>> =
            unSynced.partition { it.isDeleted }

        val deletedTransactionIds: List<String> = deletionsAndUpdates.first.map { it.transactionId }
        remoteDataSource.deleteMultipleRows(deletedTransactionIds)
        deletedTransactionIds.forEach { localDataSource.hardDelete(it) }

        updateRemote(deletionsAndUpdates.second)
        updateLocal(deletionsAndUpdates.second)
    }

    private suspend fun updateLocal(unSyncedTransactions: List<TransactionEntity>) {
        unSyncedTransactions.forEach { localDataSource.markAsSynced(it.transactionId) }
    }

    private suspend fun updateRemote(unSyncedTransactions: List<TransactionEntity>) {
        val networkTransactions: List<NetworkTransaction> = unSyncedTransactions.map { it.asNetworkModel(userId = "") }
        remoteDataSource.upsert(networkTransactions)
    }
}
