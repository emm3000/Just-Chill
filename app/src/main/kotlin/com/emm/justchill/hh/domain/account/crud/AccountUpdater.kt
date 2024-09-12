package com.emm.justchill.hh.domain.account.crud

import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.domain.account.AccountUpsert

class AccountUpdater(private val repository: AccountRepository) {

    suspend fun update(accountId: String, account: AccountUpsert) {
        repository.update(accountId, account)
    }
}