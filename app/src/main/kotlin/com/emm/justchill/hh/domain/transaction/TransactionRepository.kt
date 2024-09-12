package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.domain.transaction.model.Transaction
import com.emm.justchill.hh.domain.transaction.model.TransactionInsert
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun create(transactionInsert: TransactionInsert)

    fun findBy(transactionId: String): Flow<Transaction?>

    fun retrieve(): Flow<List<Transaction>>

    fun sumIncome(): Flow<Double>

    fun sumSpend(): Flow<Double>

    fun difference(): Flow<Double>

   suspend fun deleteBy(transactionId: String)
}