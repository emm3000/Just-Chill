package com.emm.data.transaction

class TransactionRemoteDataSource() {

    suspend fun upsert(transactions: List<TransactionModel>) {
    }

    suspend fun all(accounts: List<String>): List<TransactionModel> = listOf()
}