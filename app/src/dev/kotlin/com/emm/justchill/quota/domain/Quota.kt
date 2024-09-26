package com.emm.justchill.quota.domain

data class Quota(
    val quoteId: String,
    val amount: Double,
    val quoteDate: Long,
    val driverId: Long,
)