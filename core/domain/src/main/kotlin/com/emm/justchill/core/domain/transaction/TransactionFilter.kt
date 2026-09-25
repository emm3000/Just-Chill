package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money

data class TransactionFilter(
    val query: String = "",
    val categoryIds: Set<CategoryId> = emptySet(),
    val minAmount: Money? = null,
    val maxAmount: Money? = null,
) {
    val isEmpty: Boolean
        get() = query.isBlank() && categoryIds.isEmpty() && minAmount == null && maxAmount == null

    companion object {
        val None = TransactionFilter()
    }
}

// The one place a min greater than a max is corrected instead of rejected: a bound is set only
// through AmountInputSheet, so this is reached from user input, never from a constructed literal.
fun TransactionFilter.withAmountRange(minAmount: Money?, maxAmount: Money?): TransactionFilter {
    val swapped = minAmount != null && maxAmount != null && minAmount.cents > maxAmount.cents
    return copy(
        minAmount = if (swapped) maxAmount else minAmount,
        maxAmount = if (swapped) minAmount else maxAmount,
    )
}
