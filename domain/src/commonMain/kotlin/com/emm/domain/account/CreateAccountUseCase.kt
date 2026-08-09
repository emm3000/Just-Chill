package com.emm.domain.account

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

class CreateAccountUseCase(private val repository: AccountRepository, private val uniqueIdProvider: UniqueIdProvider) {

    suspend operator fun invoke(name: String, type: AccountType = AccountType.Bank) {
        if (name.isBlank()) {
            throw DomainException.ValidationError("Name cannot be empty", ValidationCode.NameRequired)
        }
        val accountUpsert = AccountUpsert(
            accountId = AccountId(uniqueIdProvider.id),
            name = name,
            type = type,
        )
        repository.create(accountUpsert)
    }
}
