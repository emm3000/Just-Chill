package com.emm.justchill.core.domain.shared.backup

interface BackupUploader {

    suspend fun upload(userId: String, fileName: String, payload: String)
}
