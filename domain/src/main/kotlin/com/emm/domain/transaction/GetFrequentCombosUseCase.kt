package com.emm.domain.transaction

import com.emm.domain.shared.currentTimeInMillis

class GetFrequentCombosUseCase(private val transactionStatsRepository: TransactionStatsRepository) {
    suspend operator fun invoke(
        type: TransactionType,
        windowDays: Int = WINDOW_DAYS,
        limit: Int = DEFAULT_LIMIT,
    ): List<FrequentCombo> {
        val startInclusive = currentTimeInMillis() - windowDays.toLong() * MILLIS_PER_DAY
        return transactionStatsRepository.topUsedCombos(type, startInclusive, limit)
    }

    private companion object {
        const val WINDOW_DAYS = 90
        const val DEFAULT_LIMIT = 5
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
