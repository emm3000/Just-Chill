package com.emm.domain.transaction

interface TransactionBackupRepository {

    suspend fun seed()

    suspend fun backup()
}