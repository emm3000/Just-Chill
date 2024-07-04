package com.emm.justchill.hh.data.transaction

interface TransactionSaver {

    suspend fun save(entity: TransactionInsert)
}