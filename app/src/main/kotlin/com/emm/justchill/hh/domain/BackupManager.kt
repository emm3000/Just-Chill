package com.emm.justchill.hh.domain

import kotlinx.coroutines.flow.Flow

interface BackupManager {

    suspend fun backup(): Flow<Boolean>

    suspend fun seed(): Boolean
}