package com.emm.justchill.experiences.hh.domain.transactioncategory

interface TransactionCategoryRepository {

    suspend fun add(transactionId: String, categoryId: String)
}