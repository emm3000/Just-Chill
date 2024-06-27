package com.emm.justchill.experiences.hh.data.transaction

interface TransactionSaver {

    suspend fun save(entity: TransactionInsert)
}