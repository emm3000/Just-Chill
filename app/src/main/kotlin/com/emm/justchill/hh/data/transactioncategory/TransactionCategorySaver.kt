package com.emm.justchill.hh.data.transactioncategory

interface TransactionCategorySaver {

    suspend fun save(transactionId: String, categoryId: String)
}