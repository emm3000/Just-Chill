package com.emm.justchill.hh.transaction.domain

interface TransactionBackupRepository {

    suspend fun seed()

    suspend fun backup()
}