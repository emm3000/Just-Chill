package com.emm.justchill.hh.data.transaction

interface TransactionRemoteRepository {

    suspend fun upsert(transaction: TransactionModel)

    suspend fun upsert(transactions: List<TransactionModel>)

    suspend fun retrieve(): List<TransactionModel>

    suspend fun deleteBy(transactionId: String)

    suspend fun deleteAll()
}