package com.emm.justchill.core.domain.account

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode

class UpdateAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(accountId: AccountId, account: AccountUpsert) {
        if (account.name.isBlank()) {
            throw DomainException.ValidationError("Name cannot be empty", ValidationCode.NameRequired)
        }
        repository.update(accountId, account)
    }
}
