package com.emm.domain.transaction

import com.emm.domain.shared.startOfDayDaysAgo
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class GetFrequentCombosUseCase(
    private val transactionStatsRepository: TransactionStatsRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    suspend operator fun invoke(
        type: TransactionType,
        windowDays: Int = WINDOW_DAYS,
        limit: Int = DEFAULT_LIMIT,
    ): List<FrequentCombo> {
        val startInclusive = startOfDayDaysAgo(windowDays, clock, zone)
        return transactionStatsRepository.topUsedCombos(type, startInclusive, limit)
    }

    private companion object {
        const val WINDOW_DAYS = 90
        const val DEFAULT_LIMIT = 5
    }
}
