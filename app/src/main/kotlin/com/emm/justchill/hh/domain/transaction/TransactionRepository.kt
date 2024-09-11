package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.domain.transaction.model.Transaction
import com.emm.justchill.hh.domain.transaction.model.TransactionInsert
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun add(entity: TransactionInsert)

    fun find(transactionId: String): Flow<Transaction?>

    fun all(): Flow<List<Transaction>>

    fun sumIncome(): Flow<Double>

    fun sumSpend(): Flow<Double>

    fun difference(): Flow<Double>

   suspend fun delete(transactionId: String)
}