package com.emm.justchill.hh.data.transactioncategory

import com.emm.justchill.hh.domain.TransactionCategoryModel

interface TransactionCategorySeeder {

    suspend fun seed(data: List<TransactionCategoryModel>)
}