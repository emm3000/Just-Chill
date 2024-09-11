package com.emm.justchill.hh.domain.transaction

interface TransactionSyncer {

    fun sync(transactionId: String)
}