package com.emm.domain.transaction

import kotlinx.coroutines.flow.Flow

class SearchTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(filter: TransactionFilter): Flow<List<TransactionWithCategory>> =
        transactionRepository.searchWithCategory(filter)
}
