package com.emm.justchill.hh.account.domain

import com.emm.justchill.hh.shared.UniqueIdProvider

class AccountCreator(
    private val repository: AccountRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(accountUpsert: AccountUpsert) {
        val uniqueId: String = uniqueIdProvider.uniqueId
        repository.create(uniqueId, accountUpsert)
    }
}