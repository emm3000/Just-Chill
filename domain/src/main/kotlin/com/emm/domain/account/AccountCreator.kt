package com.emm.domain.account

import com.emm.domain.shared.UniqueIdProvider

class AccountCreator(
    private val repository: AccountRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(name: String, balance: Double) {
        val accountUpsert = AccountUpsert(
            accountId = uniqueIdProvider.id,
            name = name,
            balance = balance,
        )
        repository.create(accountUpsert)
    }
}