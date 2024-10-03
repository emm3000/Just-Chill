package com.emm.justchill.daily.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.Dailies
import com.emm.justchill.DailiesQueries
import com.emm.justchill.EmmDatabase
import com.emm.justchill.daily.domain.Daily
import com.emm.justchill.daily.domain.DailyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalDailyRepository(private val emmDatabase: EmmDatabase) : DailyRepository {

    private val dq: DailiesQueries
        get() = emmDatabase.dailiesQueries

    override suspend fun insert(daily: Daily) = withContext(Dispatchers.IO) {
        dq.insert(
            dailyId = daily.dailyId,
            amount = daily.amount,
            dailyDate = daily.dailyDate,
            driverId = daily.driverId
        )
    }

    override fun all(): Flow<List<Daily>> {
        return dq.retrieve()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    override fun retrieveBy(driverId: Long): Flow<List<Daily>> {
        return dq.find(driverId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    override suspend fun deleteBy(dailyId: String) = withContext(Dispatchers.IO) {
        dq.delete(dailyId)
    }

    private fun toDomain(list: List<Dailies>): List<Daily> {
        return list.map {
            Daily(
                dailyId = it.dailyId,
                amount = it.amount,
                dailyDate = it.dailyDate,
                driverId = it.driverId
            )
        }
    }
}