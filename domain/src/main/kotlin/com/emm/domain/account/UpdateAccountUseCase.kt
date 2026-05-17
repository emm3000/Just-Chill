package com.emm.domain.account

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.error.DomainException

class UpdateAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(accountId: AccountId, account: AccountUpsert) {
        if (account.name.isBlank()) {
            throw DomainException.ValidationError("El nombre no puede estar vacío")
        }
        repository.update(accountId, account)
    }
}
