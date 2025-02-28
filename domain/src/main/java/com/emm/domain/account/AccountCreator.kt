package com.emm.domain.account

import com.emm.domain.shared.UniqueIdProvider

class AccountCreator(
    private val repository: AccountRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(accountUpsert: AccountUpsert) {
        val uniqueId: String = uniqueIdProvider.uniqueId
        repository.create(uniqueId, accountUpsert)
    }
}