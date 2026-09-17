package com.emm.justchill.core.domain.shared.backup

interface BackupEraser {

    // Returns only when every object under userId's own Storage prefix is gone; the throw is the
    // contract, stopping an account deletion that would strand them forever. userId is the account
    // the caller resolved, not a hint: implementations refuse to delete unless the live session owns it.
    suspend fun eraseOwnedBackups(userId: String)
}
