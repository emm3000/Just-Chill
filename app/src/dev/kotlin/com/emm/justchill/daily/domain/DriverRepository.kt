package com.emm.justchill.daily.domain

import kotlinx.coroutines.flow.Flow

interface DriverRepository {

    suspend fun insert(driver: Driver)

    fun all(): Flow<List<Driver>>

    fun find(driverId: Long): Flow<Driver?>
}