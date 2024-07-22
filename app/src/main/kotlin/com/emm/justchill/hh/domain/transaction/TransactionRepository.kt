package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.Ga
import com.emm.justchill.Transactions
import com.emm.justchill.hh.data.transaction.TransactionInsert
import com.emm.justchill.hh.data.transaction.TransactionUpdate
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun add(entity: TransactionInsert)

    suspend fun seed()

    suspend fun backup()

    suspend fun find(transactionId: String): Ga?

    suspend fun update(transactionId: String, transactionUpdate: TransactionUpdate)

    fun all(): Flow<List<Transactions>>

    fun retrieveWithLimit(limit: Long, offset: Long): Flow<List<Transactions>>

    fun retrieveByDateRange(
        startDateMillis: Long,
        endDateMillis: Long,
    ): Flow<List<Transactions>>

    fun retrieveByDateRangeWithLimit(
        startDateMillis: Long,
        endDateMillis: Long,
        limit: Long,
        offset: Long
    ): Flow<List<Transactions>>

    fun sumIncome(): Flow<Long>

    fun sumSpend(): Flow<Long>

    fun difference(): Flow<Long>

   suspend fun delete(transactionId: String)
}