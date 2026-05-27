package com.emm.domain.transaction

import com.emm.domain.shared.AccountId

class GetLastUsedAccountIdUseCase(private val transactionStatsRepository: TransactionStatsRepository) {
    suspend operator fun invoke(): AccountId? = transactionStatsRepository.lastUsedAccountId()
}
