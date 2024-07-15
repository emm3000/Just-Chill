package com.emm.justchill.hh.domain.transactioncategory

import com.emm.justchill.TransactionsCategories
import kotlinx.coroutines.flow.Flow

interface TransactionCategoryRepository {

    suspend fun add(transactionId: String, categoryId: String)

    fun retrieve(): Flow<List<TransactionsCategories>>

    suspend fun seed()

    suspend fun backup()
}