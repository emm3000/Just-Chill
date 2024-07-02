package com.emm.justchill.experiences.hh.data.transaction

import kotlinx.coroutines.flow.Flow

interface TransactionCalculator {

    fun sumSpend(): Flow<Long>

    fun sumIncome(): Flow<Long>

    fun difference(): Flow<Long>
}