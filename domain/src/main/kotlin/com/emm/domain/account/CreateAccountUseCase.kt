package com.emm.domain.account

import com.emm.domain.shared.UniqueIdProvider

class CreateAccountUseCase(
    private val repository: AccountRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend operator fun invoke(name: String) {
        val accountUpsert = AccountUpsert(
            accountId = uniqueIdProvider.id,
            name = name,
        )
        repository.create(accountUpsert)
    }
}
