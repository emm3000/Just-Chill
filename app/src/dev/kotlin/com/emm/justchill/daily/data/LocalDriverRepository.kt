package com.emm.justchill.daily.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.justchill.Drivers
import com.emm.justchill.DriversQueries
import com.emm.justchill.EmmDatabase
import com.emm.justchill.daily.domain.Driver
import com.emm.justchill.daily.domain.DriverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalDriverRepository(private val emmDatabase: EmmDatabase) : DriverRepository {

    private val dq: DriversQueries
        get() = emmDatabase.driversQueries

    override suspend fun insert(driver: Driver) = withContext(Dispatchers.IO) {
        dq.insert(driver.driverId, driver.name)
    }

    override fun all(): Flow<List<Driver>> {
        return dq.retrieve()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    override fun find(driverId: Long): Flow<Driver?> {
        return dq.find(driverId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map {
                it?.let {
                    Driver(
                        driverId = it.driverId,
                        name = it.name
                    )
                }
            }
    }

    private fun toDomain(drivers: List<Drivers>): List<Driver> {
        return drivers.map {
            Driver(
                driverId = it.driverId,
                name = it.name
            )
        }
    }
}