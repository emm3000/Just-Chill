package com.emm.justchill.hh.data.transaction

import com.emm.justchill.hh.domain.TransactionModel

interface TransactionNetworkDataSource {

    suspend fun upsert(transaction: TransactionModel)

    suspend fun upsert(transactions: List<TransactionModel>)

    suspend fun retrieve(): List<TransactionModel>

    suspend fun deleteAll()
}