package com.emm.justchill.experiences.hh.data

interface TransactionCategorySaver {

    suspend fun save(transactionId: String, categoryId: String)
}