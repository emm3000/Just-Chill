package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.Money

data class TransactionTotals(val balance: Money, val movementCount: Long) {
    companion object {
        val Empty: TransactionTotals = TransactionTotals(Money.Zero, 0L)
    }
}
