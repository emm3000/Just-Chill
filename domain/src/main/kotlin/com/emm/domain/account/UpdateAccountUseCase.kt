package com.emm.domain.account

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

class UpdateAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(accountId: AccountId, account: AccountUpsert) {
        if (account.name.isBlank()) {
            throw DomainException.ValidationError("Name cannot be empty", ValidationCode.NameRequired)
        }
        repository.update(accountId, account)
    }
}
