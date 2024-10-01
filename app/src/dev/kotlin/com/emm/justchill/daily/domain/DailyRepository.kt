package com.emm.justchill.daily.domain

import kotlinx.coroutines.flow.Flow

interface DailyRepository {

    suspend fun insert(daily: Daily)

    fun all(): Flow<List<Daily>>

    fun retrieveBy(driverId: Long): Flow<List<Daily>>
}