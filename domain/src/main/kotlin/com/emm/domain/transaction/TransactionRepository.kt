package com.emm.domain.transaction

import com.emm.domain.shared.TransactionId
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun create(transactionInsert: TransactionInsert)

    fun find(transactionId: TransactionId): Transaction?

    fun all(): Flow<List<Transaction>>

    fun fetchAllWithCategory(): Flow<List<TransactionWithCategory>>

    suspend fun update(transactionId: TransactionId, transactionUpdate: TransactionUpdate)

    suspend fun delete(transactionId: TransactionId)

    suspend fun pull()

    suspend fun sync()
}