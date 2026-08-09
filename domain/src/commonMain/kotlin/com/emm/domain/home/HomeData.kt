package com.emm.domain.home

import com.emm.domain.recurring.PendingRecurring
import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionWithCategory

data class HomeData(
    val lastTransactions: List<TransactionWithCategory>,
    val income: Money,
    val spend: Money,
    val balance: Money,
    val hasAnyTransaction: Boolean,
    /**
     * Everything owed as of today, oldest period first. Deliberately NOT scoped to the month the
     * user is looking at: it is a to-do list, and a month missed while the app was closed has to
     * stay visible after the calendar moves on.
     */
    val pendingRecurringMovements: List<PendingRecurring> = emptyList(),
)
