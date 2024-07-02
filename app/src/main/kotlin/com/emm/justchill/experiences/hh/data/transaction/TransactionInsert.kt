package com.emm.justchill.experiences.hh.data.transaction

data class TransactionInsert(
    val id: String? = null,
    val type: String,
    val amount: Long = 0L,
    val description: String,
    val date: Long,
)
