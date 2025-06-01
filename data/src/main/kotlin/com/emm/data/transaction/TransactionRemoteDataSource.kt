package com.emm.data.transaction

class TransactionRemoteDataSource {

    suspend fun upsert(transaction: TransactionModel) {}

    suspend fun upsert(transactions: List<TransactionModel>) {}

    suspend fun retrieve(): List<TransactionModel> = listOf()

    suspend fun delete(transactionId: String) {}

    suspend fun deleteAll() {}
}