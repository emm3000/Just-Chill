package com.emm.domain.transaction

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.startOfDayDaysAgo
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class GetTopUsedCategoryIdsUseCase(
    private val transactionStatsRepository: TransactionStatsRepository,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    suspend operator fun invoke(
        type: TransactionType,
        windowDays: Int = WINDOW_DAYS,
        limit: Int = DEFAULT_LIMIT,
    ): List<CategoryId> {
        val startInclusive = startOfDayDaysAgo(windowDays, clock, zone)
        return transactionStatsRepository.topUsedCategoryIds(type, startInclusive, limit)
    }

    private companion object {
        const val WINDOW_DAYS = 90
        const val DEFAULT_LIMIT = 6
    }
}
