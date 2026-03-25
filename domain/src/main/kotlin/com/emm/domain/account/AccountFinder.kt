package com.emm.domain.account

class FindAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(accountId: String): Account? {
        return repository.find(accountId)
    }
}
