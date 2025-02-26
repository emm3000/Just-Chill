package com.emm.justchill.hh.me.daily.domain

data class Daily(
    val dailyId: String,
    val amount: Double,
    val dailyDate: Long,
    val driverId: Long,
)