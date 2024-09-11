package com.emm.justchill.hh.domain.transaction

interface TransactionBackupRepository {

    suspend fun seed()

    suspend fun backup()
}