package com.emm.justchill.experiences.hh.data.transactioncategory

interface TransactionCategorySaver {

    suspend fun save(transactionId: String, categoryId: String)
}