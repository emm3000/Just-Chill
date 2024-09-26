package com.emm.justchill.quota.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.EmmDatabase
import com.emm.justchill.Quotas
import com.emm.justchill.QuotesQueries
import com.emm.justchill.quota.domain.Quota
import com.emm.justchill.quota.domain.QuotaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalQuotaRepository(private val emmDatabase: EmmDatabase) : QuotaRepository {

    private val qq: QuotesQueries
        get() = emmDatabase.quotesQueries

    override suspend fun insert(quota: Quota) = withContext(Dispatchers.IO) {
        qq.insert(
            quoteId = quota.quoteId,
            amount = quota.amount,
            quoteDate = quota.quoteDate,
            driverId = quota.driverId
        )
    }

    override fun all(): Flow<List<Quota>> {
        return qq.retrieve()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    override fun retrieveBy(driverId: Long): Flow<List<Quota>> {
        return qq.find(driverId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    private fun toDomain(list: List<Quotas>): List<Quota> {
        return list.map {
            Quota(
                quoteId = it.quoteId,
                amount = it.amount,
                quoteDate = it.quoteDate,
                driverId = it.driverId
            )
        }
    }
}