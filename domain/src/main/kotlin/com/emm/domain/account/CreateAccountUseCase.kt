package com.emm.domain.account

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException

class CreateAccountUseCase(private val repository: AccountRepository, private val uniqueIdProvider: UniqueIdProvider) {

    suspend operator fun invoke(
        name: String,
        type: AccountType = AccountType.Bank,
        currency: Currency = Currency.PEN,
    ) {
        if (name.isBlank()) {
            throw DomainException.ValidationError("El nombre no puede estar vacío")
        }
        val accountUpsert = AccountUpsert(
            accountId = AccountId(uniqueIdProvider.id),
            name = name,
            type = type,
            currency = currency,
        )
        repository.create(accountUpsert)
    }
}
