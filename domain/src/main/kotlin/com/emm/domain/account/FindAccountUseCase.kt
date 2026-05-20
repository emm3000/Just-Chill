package com.emm.domain.account

import com.emm.domain.shared.AccountId

class FindAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(accountId: AccountId): Account? = repository.find(accountId)
}
