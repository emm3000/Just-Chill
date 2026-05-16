package com.emm.domain.account

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.UniqueIdProvider

class CreateAccountUseCase(
    private val repository: AccountRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend operator fun invoke(
        name: String,
        type: AccountType = AccountType.Bank,
        currency: Currency = Currency.ARS,
    ) {
        val accountUpsert = AccountUpsert(
            accountId = AccountId(uniqueIdProvider.id),
            name = name,
            type = type,
            currency = currency,
        )
        repository.create(accountUpsert)
    }
}
