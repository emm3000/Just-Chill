package com.emm.domain.transaction

import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun create(transactionInsert: TransactionInsert)

    fun find(transactionId: String): Transaction?

    fun all(): Flow<List<Transaction>>

    fun fetchAllWithCategory(): Flow<List<TransactionWithCategory>>

    suspend fun delete(transactionId: String)

    suspend fun pull()

    suspend fun sync()
}