package com.emm.justchill.hh.data.transaction

import com.emm.justchill.hh.domain.TransactionModel

interface TransactionSeeder {

    suspend fun seed(data: List<TransactionModel>)
}