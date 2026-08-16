package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class GetTopCategoriesOverMonthsUseCase(private val transactionStatsRepository: TransactionStatsRepository) {

    suspend operator fun invoke(
        type: TransactionType,
        clock: Clock,
        zone: TimeZone,
        months: Int = 6,
        topN: Int = 3,
    ): List<CategoryAggregate> {
        val window = YearMonth.windowEndingAt(YearMonth.current(clock, zone), months)

        val monthlyResults: List<List<CategoryAmount>> = transactionStatsRepository
            .monthlyAmountByCategoryForRanges(window.map { it.range() })
            .map { slice -> slice.of(type).filter { amount -> amount.categoryId != null } }

        val totals = mutableMapOf<CategoryId, Money>()
        val meta = mutableMapOf<CategoryId, Triple<String, String, String>>()

        monthlyResults.forEach { items ->
            items.forEach { item ->
                val id: CategoryId = item.categoryId ?: return@forEach
                totals[id] = (totals[id] ?: Money.Zero) + item.amount
                meta[id] = Triple(
                    item.categoryName.orEmpty(),
                    item.categoryColor.orEmpty(),
                    item.categoryIcon.orEmpty(),
                )
            }
        }

        val localTopSets: List<Set<CategoryId>> = monthlyResults.map { items ->
            items.sortedByDescending { it.amount.cents }.take(topN).mapNotNull { it.categoryId }.toSet()
        }

        val monthsInTop = mutableMapOf<CategoryId, Int>()
        totals.keys.forEach { catId ->
            val count = localTopSets.count { it.contains(catId) }
            monthsInTop[catId] = count
        }

        return totals.entries
            .sortedByDescending { it.value.cents }
            .take(topN)
            .mapNotNull { (catId, total) ->
                val (name, color, icon) = meta[catId] ?: return@mapNotNull null
                CategoryAggregate(
                    categoryId = catId,
                    categoryName = name,
                    categoryColor = color,
                    categoryIcon = icon,
                    totalAmount = total,
                    monthsInTop = monthsInTop[catId] ?: 0,
                    totalMonths = months,
                )
            }
    }
}
