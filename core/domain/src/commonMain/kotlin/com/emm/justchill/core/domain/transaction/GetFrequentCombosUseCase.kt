package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.startOfDayDaysAgo
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
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
        amount: Money? = null,
    ): List<FrequentCombo> {
        val startInclusive: String = startOfDayDaysAgo(windowDays, clock, zone)
        if (amount == null || amount == Money.Zero) {
            return transactionStatsRepository.topUsedCombos(type, startInclusive, limit)
        }
        val occurrences: List<ComboOccurrence> = transactionStatsRepository.comboOccurrences(type, startInclusive)
        return rankByContext(occurrences, amount, limit)
    }

    private fun rankByContext(occurrences: List<ComboOccurrence>, amount: Money, limit: Int): List<FrequentCombo> {
        val currentHour: Int = clock.now().toLocalDateTime(zone).hour
        val amountBandRadiusCents: Long = amountBandRadiusCents(amount)

        return occurrences
            .groupBy { FrequentCombo(it.accountId, it.categoryId, it.type) }
            .map { (combo, rows) ->
                RankedCombo(
                    combo = combo,
                    count = rows.size,
                    matchesContext = rows.any { matchesContext(it, amount, amountBandRadiusCents, currentHour) },
                    lastOccurredAt = rows.maxOf { it.occurredAt },
                )
            }
            .sortedWith(
                compareByDescending<RankedCombo> { it.matchesContext }
                    .thenByDescending { it.count }
                    .thenByDescending { it.lastOccurredAt },
            )
            .take(limit)
            .map { it.combo }
    }

    private fun matchesContext(
        occurrence: ComboOccurrence,
        typedAmount: Money,
        amountBandRadiusCents: Long,
        currentHour: Int,
    ): Boolean {
        val occurrenceHour: Int = parseHourOrNull(occurrence.occurredAt) ?: return false
        val withinAmountBand: Boolean = abs(occurrence.amount.cents - typedAmount.cents) <= amountBandRadiusCents
        return withinAmountBand && matchesHourWindow(occurrenceHour, currentHour)
    }

    private fun amountBandRadiusCents(amount: Money): Long =
        (amount.cents * AMOUNT_BAND_FRACTION).toLong().coerceAtLeast(AMOUNT_BAND_MIN_CENTS)

    private fun parseHourOrNull(occurredAt: String): Int? =
        runCatching { LocalDateTime.parse(occurredAt).hour }.getOrNull()

    private fun matchesHourWindow(occurrenceHour: Int, currentHour: Int): Boolean =
        circularHourDistance(occurrenceHour, currentHour) <= HOUR_WINDOW_RADIUS

    private fun circularHourDistance(a: Int, b: Int): Int {
        val diff: Int = abs(a - b)
        return minOf(diff, HOURS_IN_DAY - diff)
    }

    private data class RankedCombo(
        val combo: FrequentCombo,
        val count: Int,
        val matchesContext: Boolean,
        val lastOccurredAt: String,
    )

    private companion object {
        const val WINDOW_DAYS = 90
        const val DEFAULT_LIMIT = 5
        const val AMOUNT_BAND_FRACTION = 0.2
        const val AMOUNT_BAND_MIN_CENTS = 200L
        const val HOUR_WINDOW_RADIUS = 2
        const val HOURS_IN_DAY = 24
    }
}
