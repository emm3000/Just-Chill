package com.emm.domain.transaction

import com.emm.domain.shared.Money

/**
 * Whole-ledger aggregates, computed by the database rather than by folding every row in memory.
 *
 * [movementCount] counts only rows the app can interpret, which is what makes
 * `movementCount > 0` equivalent to "some list in the app has something in it".
 */
data class TransactionTotals(val balance: Money, val movementCount: Long) {
    companion object {
        val Empty: TransactionTotals = TransactionTotals(Money.Zero, 0L)
    }
}
