package com.emm.justchill.me.daily.domain

import kotlinx.coroutines.flow.Flow

interface DailyRepository {

    suspend fun insert(daily: Daily)

    fun all(): Flow<List<Daily>>

    fun retrieve(driverId: Long): Flow<List<Daily>>

    suspend fun delete(dailyId: String)
}