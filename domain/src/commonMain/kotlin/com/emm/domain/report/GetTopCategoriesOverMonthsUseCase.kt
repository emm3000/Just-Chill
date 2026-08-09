package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType
import kotlin.time.Clock

class GetTopCategoriesOverMonthsUseCase(private val transactionStatsRepository: TransactionStatsRepository) {

    suspend operator fun invoke(
        type: TransactionType,
        months: Int = 6,
        topN: Int = 3,
        clock: Clock = Clock.System,
    ): List<CategoryAggregate> {
        val current = YearMonth.current(clock)

        // Collect per-month results oldest-first.
        // The uncategorized bucket is dropped here: this is a ranking OF categories, and a
        // bucket with no name, icon or color cannot occupy one of the top-N slots. Month
        // totals still include it — see [CategoryAmount].
        val monthlyResults = mutableListOf<List<CategoryAmount>>()
        var ym = current
        repeat(months) {
            val start = ym.startInclusiveMillis()
            val end = ym.endExclusiveMillis()
            val items = transactionStatsRepository.monthlyAmountByCategory(type, start, end)
                .filter { amount -> amount.categoryId != null }
            monthlyResults.add(0, items)
            ym = ym.previous()
        }

        // Aggregate totals across all months per categoryId
        val totals = mutableMapOf<CategoryId, Money>()
        val meta = mutableMapOf<CategoryId, Triple<String, String, String>>() // name, color, icon

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

        // For each month, identify the local top-N categoryIds
        val localTopSets: List<Set<CategoryId>> = monthlyResults.map { items ->
            items.sortedByDescending { it.amount.cents }.take(topN).mapNotNull { it.categoryId }.toSet()
        }

        // Count how many months each aggregated category appeared in the local top-N
        val monthsInTop = mutableMapOf<CategoryId, Int>()
        totals.keys.forEach { catId ->
            val count = localTopSets.count { it.contains(catId) }
            monthsInTop[catId] = count
        }

        // Return top-N overall by total amount
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
