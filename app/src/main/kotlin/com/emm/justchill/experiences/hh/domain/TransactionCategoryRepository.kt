package com.emm.justchill.experiences.hh.domain

interface TransactionCategoryRepository {

    suspend fun add(transactionId: String, categoryId: String)
}