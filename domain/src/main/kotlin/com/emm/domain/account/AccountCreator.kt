package com.emm.domain.account

import com.emm.domain.shared.SyncState
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.currentTimeInMillis

class AccountCreator(
    private val repository: AccountRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(name: String, balance: Double) {
        val accountUpsert = AccountUpsert(
            accountId = uniqueIdProvider.id,
            name = name,
            balance = balance,
            updatedAt = currentTimeInMillis(),
            createdAt = currentTimeInMillis(),
            syncState = SyncState.Pending,
        )
        repository.create(accountUpsert)
    }
}