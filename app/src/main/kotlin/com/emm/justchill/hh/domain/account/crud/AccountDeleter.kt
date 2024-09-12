package com.emm.justchill.hh.domain.account.crud

import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.domain.transaction.SyncStatus

class AccountDeleter(private val repository: AccountRepository) {

    suspend fun delete(accountId: String) {
        repository.updateStatus(accountId, SyncStatus.PENDING_DELETE)
    }
}