package com.emm.justchill.hh.data.transaction

interface TransactionSaver {

    suspend fun save(entity: com.emm.justchill.hh.data.transaction.TransactionInsert)
}