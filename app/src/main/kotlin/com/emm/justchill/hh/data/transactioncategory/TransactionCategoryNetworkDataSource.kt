package com.emm.justchill.hh.data.transactioncategory

import com.emm.justchill.hh.domain.TransactionCategoryModel

interface TransactionCategoryNetworkDataSource {

    suspend fun upsert(transactionCategory: TransactionCategoryModel)

    suspend fun upsert(transactionsCategories: List<TransactionCategoryModel>)

    suspend fun retrieve(): List<TransactionCategoryModel>

    suspend fun deleteAll()
}