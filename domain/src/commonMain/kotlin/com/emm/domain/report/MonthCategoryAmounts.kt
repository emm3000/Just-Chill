package com.emm.domain.report

import com.emm.domain.transaction.TransactionType

data class MonthCategoryAmounts(val income: List<CategoryAmount>, val expense: List<CategoryAmount>) {
    fun of(type: TransactionType): List<CategoryAmount> = when (type) {
        TransactionType.Income -> income
        TransactionType.Spend -> expense
    }

    companion object {
        val Empty: MonthCategoryAmounts = MonthCategoryAmounts(emptyList(), emptyList())
    }
}
