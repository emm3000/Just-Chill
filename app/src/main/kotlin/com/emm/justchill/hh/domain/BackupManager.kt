package com.emm.justchill.hh.domain

interface BackupManager {

    suspend fun backup(): Boolean

    suspend fun seed(): Boolean
}