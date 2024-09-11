package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.hh.domain.transaction.model.TransactionInsert
import com.emm.justchill.hh.domain.transaction.model.TransactionUpdate
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun add(entity: TransactionInsert)

    suspend fun seed()

    suspend fun backup()

    fun find(transactionId: String): Flow<Transactions?>

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

    fun sumIncome(): Flow<Double>

    fun sumSpend(): Flow<Double>

    fun difference(): Flow<Double>

   suspend fun delete(transactionId: String)
}