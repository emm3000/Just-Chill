package com.emm.justchill.hh.domain.shared

interface BackupManager {

    suspend fun backup(): Boolean

    suspend fun seed(): Boolean
}