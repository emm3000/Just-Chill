package com.emm.justchill.hh.account.domain

import com.emm.justchill.hh.transaction.domain.SyncStatus

class AccountDeleter(private val repository: AccountRepository) {

    suspend fun delete(accountId: String) {
        repository.updateStatus(accountId, SyncStatus.PENDING_DELETE)
    }
}