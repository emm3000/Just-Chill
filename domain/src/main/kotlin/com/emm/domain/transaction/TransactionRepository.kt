package com.emm.domain.transaction

import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun create(transactionInsert: TransactionInsert)

    fun findBy(transactionId: String): Flow<Transaction?>

    fun retrieve(): Flow<List<Transaction>>

    fun sumIncome(accountId: String): Flow<Double>

    fun sumSpend(accountId: String): Flow<Double>

    fun difference(accountId: String): Flow<Double>

   suspend fun deleteBy(transactionId: String)
}