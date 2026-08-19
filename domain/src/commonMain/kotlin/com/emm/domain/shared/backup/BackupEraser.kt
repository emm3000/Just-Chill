package com.emm.domain.shared.backup

interface BackupEraser {

    /**
     * Returns only when every object under [userId]'s own Storage prefix is gone; the throw is the
     * contract, because it is what stops an account deletion that would strand them forever.
     *
     * [userId] is the account the caller resolved, not a hint: the implementation refuses to delete
     * anything unless the live session still owns that prefix.
     */
    suspend fun eraseOwnedBackups(userId: String)
}
