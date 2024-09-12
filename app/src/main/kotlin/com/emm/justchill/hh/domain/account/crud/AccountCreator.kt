package com.emm.justchill.hh.domain.account.crud

import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.domain.account.AccountUpsert
import com.emm.justchill.hh.domain.shared.UniqueIdProvider

class AccountCreator(
    private val repository: AccountRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(accountUpsert: AccountUpsert) {
        val uniqueId: String = uniqueIdProvider.uniqueId
        repository.create(uniqueId, accountUpsert)
    }
}