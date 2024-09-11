package com.emm.justchill.hh.data.transaction

import com.emm.justchill.hh.domain.transaction.TransactionModel

interface TransactionSupabaseRepository {

    suspend fun upsert(transaction: TransactionModel)

    suspend fun upsert(transactions: List<TransactionModel>)

    suspend fun retrieve(): List<TransactionModel>

    suspend fun deleteBy(transactionId: String)

    suspend fun deleteAll()
}