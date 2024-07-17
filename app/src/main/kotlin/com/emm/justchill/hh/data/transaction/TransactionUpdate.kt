package com.emm.justchill.hh.data.transaction

data class TransactionUpdate(
    val type: String,
    val amount: Long,
    val description: String,
    val date: Long,
)
