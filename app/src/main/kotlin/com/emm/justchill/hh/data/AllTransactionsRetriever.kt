package com.emm.justchill.hh.data

import com.emm.justchill.Transactions
import kotlinx.coroutines.flow.Flow

interface AllTransactionsRetriever {

    fun retrieve(): Flow<List<Transactions>>

    fun retrieveByDateRange(
        startDateMillis: Long,
        endDateMillis: Long
    ): Flow<List<Transactions>>
}