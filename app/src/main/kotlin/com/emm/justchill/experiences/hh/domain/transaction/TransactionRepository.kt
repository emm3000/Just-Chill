package com.emm.justchill.experiences.hh.domain.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.experiences.hh.data.transaction.TransactionInsert
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun add(entity: TransactionInsert)

    fun all(): Flow<List<Transactions>>

    fun retrieveByDateRange(
        startDateMillis: Long,
        endDateMillis: Long,
    ): Flow<List<Transactions>>

    fun sumIncome(): Flow<Long>

    fun sumSpend(): Flow<Long>

    fun difference(): Flow<Long>
}