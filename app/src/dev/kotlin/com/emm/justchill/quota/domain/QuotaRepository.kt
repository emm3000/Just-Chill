package com.emm.justchill.quota.domain

import kotlinx.coroutines.flow.Flow

interface QuotaRepository {

    suspend fun insert(quota: Quota)

    fun all(): Flow<List<Quota>>

    fun retrieveBy(driverId: Long): Flow<List<Quota>>
}