package com.emm.justchill.experiences.hh.data.transactioncategory

import com.emm.justchill.TransactionsCategories
import kotlinx.coroutines.flow.Flow

interface TransactionCategoryRetriever {

    fun retrieve(): Flow<List<TransactionsCategories>>
}