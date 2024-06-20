package com.emm.justchill.experiences.hh.data

interface TransactionSaver {

    suspend fun save(entity: TransactionInsert)
}