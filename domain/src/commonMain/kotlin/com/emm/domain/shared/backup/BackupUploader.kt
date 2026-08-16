package com.emm.domain.shared.backup

interface BackupUploader {

    suspend fun upload(userId: String, fileName: String, payload: String)
}
