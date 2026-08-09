package com.emm.domain.report

import com.emm.domain.transaction.TransactionType

/**
 * One month's category breakdown, split by transaction type.
 *
 * Both lists follow [CategoryAmount] semantics, uncategorized bucket included, so summing either
 * one yields that month's income or expense total.
 */
data class MonthCategoryAmounts(val income: List<CategoryAmount>, val expense: List<CategoryAmount>) {
    fun of(type: TransactionType): List<CategoryAmount> = when (type) {
        TransactionType.Income -> income
        TransactionType.Spend -> expense
    }

    companion object {
        val Empty: MonthCategoryAmounts = MonthCategoryAmounts(emptyList(), emptyList())
    }
}
