package com.emm.domain.transaction

import com.emm.domain.shared.Money

data class TransactionTotals(val balance: Money, val movementCount: Long) {
    companion object {
        val Empty: TransactionTotals = TransactionTotals(Money.Zero, 0L)
    }
}
