package com.emm.domain.account

class DeleteAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(accountId: String) {
        repository.delete(accountId)
    }
}
