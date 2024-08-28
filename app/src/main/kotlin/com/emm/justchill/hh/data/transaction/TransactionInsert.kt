package com.emm.justchill.hh.data.transaction

data class TransactionInsert(
    val id: String? = null,
    val type: String,
    val amount: Double = 0.0,
    val description: String,
    val date: Long,
)
