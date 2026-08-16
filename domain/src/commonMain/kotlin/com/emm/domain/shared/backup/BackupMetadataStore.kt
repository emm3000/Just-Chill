package com.emm.domain.shared.backup

interface BackupMetadataStore {

    fun lastSuccessfulBackupAt(userId: String): Long?

    fun setLastSuccessfulBackupAt(userId: String, epochMillis: Long)

    fun failureState(userId: String): BackupFailureState

    fun recordFailure(userId: String, reason: BackupFailureReason): BackupFailureState

    fun clearFailures(userId: String)

    fun destinationDisclosedAt(userId: String): Long?

    fun setDestinationDisclosed(userId: String, epochMillis: Long)

    fun clear(userId: String)
}
