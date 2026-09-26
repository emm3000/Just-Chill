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

    val amountQuery: Money?
        get() = parseAmountQuery(query)

    companion object {
        val None = TransactionFilter()
    }
}

private val AMOUNT_QUERY_PATTERN: Regex = Regex("^\\d+(\\.\\d{1,2})?$")
private const val CENTS_PER_UNIT: Long = 100L

fun parseAmountQuery(query: String): Money? {
    val withoutPrefix: String = query.trim().removePrefix("S/").trimStart()
    val normalized: String = withoutPrefix.replace(",", "")
    if (!AMOUNT_QUERY_PATTERN.matches(normalized)) return null

    val parts: List<String> = normalized.split(".")
    val majorPart: String = parts[0]
    val minorPart: String = if (parts.size > 1) parts[1].padEnd(2, '0') else "00"
    return Money(majorPart.toLong() * CENTS_PER_UNIT + minorPart.toLong())
}

fun TransactionFilter.withAmountRange(minAmount: Money?, maxAmount: Money?): TransactionFilter {
    val swapped: Boolean = minAmount != null && maxAmount != null && minAmount.cents > maxAmount.cents
    return copy(
        minAmount = if (swapped) maxAmount else minAmount,
        maxAmount = if (swapped) minAmount else maxAmount,
    )
}
