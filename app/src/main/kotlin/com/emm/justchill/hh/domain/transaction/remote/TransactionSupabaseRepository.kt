package com.emm.justchill.hh.domain.transaction.remote

import com.emm.justchill.hh.domain.transaction.TransactionModel

interface TransactionSupabaseRepository {

    suspend fun upsert(transaction: TransactionModel)

    suspend fun upsert(transactions: List<TransactionModel>)

    suspend fun retrieve(): List<TransactionModel>

    suspend fun deleteAll()
}