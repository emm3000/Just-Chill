package com.emm.justchill.experiences.hh.data

data class TransactionInsert(
    val type: String,
    val mount: String,
    val description: String,
    val date: Long,
)
