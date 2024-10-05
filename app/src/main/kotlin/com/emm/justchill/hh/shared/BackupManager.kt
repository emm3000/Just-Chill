package com.emm.justchill.hh.shared

interface BackupManager {

    suspend fun backup(): Boolean

    suspend fun seed(): Boolean
}