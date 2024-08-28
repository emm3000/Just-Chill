package com.emm.justchill.hh.data.transaction

data class TransactionUpdate(
    val type: String,
    val amount: Double,
    val description: String,
    val date: Long,
)
