package com.emm.justchill.experiences.hh.data.transaction

data class TransactionInsert(
    val id: String? = null,
    val type: String,
    val amount: String,
    val description: String,
    val date: Long,
)
