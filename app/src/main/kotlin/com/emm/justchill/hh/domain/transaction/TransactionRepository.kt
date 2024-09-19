package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.domain.transaction.model.Transaction
import com.emm.justchill.hh.domain.transaction.model.TransactionInsert
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun create(transactionInsert: TransactionInsert)

    fun findBy(transactionId: String): Flow<Transaction?>

    fun retrieve(accountId: String): Flow<List<Transaction>>

    fun sumIncome(accountId: String): Flow<Double>

    fun sumSpend(accountId: String): Flow<Double>

    fun difference(accountId: String): Flow<Double>

   suspend fun deleteBy(transactionId: String)
}