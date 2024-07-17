package com.emm.justchill.hh.data.transaction

import java.time.Instant

data class TransactionInsert(
    val id: String? = null,
    val type: String,
    val amount: Long = 0L,
    val description: String,
    val date: Long,
    val time: Long = Instant.now().toEpochMilli(),
)
