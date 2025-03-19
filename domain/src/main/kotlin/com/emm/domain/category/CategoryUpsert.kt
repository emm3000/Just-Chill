package com.emm.domain.category

import com.emm.domain.transaction.TransactionType

data class CategoryUpsert(
    val name: String,
    val description: String,
    val type: TransactionType,
)