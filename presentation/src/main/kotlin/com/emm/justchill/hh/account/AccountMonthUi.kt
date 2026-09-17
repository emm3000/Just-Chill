package com.emm.justchill.hh.account

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.Transaction
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.positiveMoneyFormatted

// An account has no opening balance, so net is a monthly net and never a balance — the screen owes
// the user the "este mes" caption that says so.
data class AccountMonthUi(val account: Account, val movementCount: Int, val net: String, val netIsPositive: Boolean)

internal data class AccountsMonthSlice(
    val month: YearMonth,
    val accounts: List<AccountMonthUi>,
    val spent: String,
    val income: String,
)

internal fun accountsMonthSlice(
    accounts: List<Account>,
    transactions: List<Transaction>,
    month: YearMonth,
): AccountsMonthSlice {
    // Load-bearing, not redundant: month reaches the caller's combine() twice, directly and through
    // the flatMapLatest re-querying transactions on it, so a rollover can emit an intermediate tuple
    // pairing the NEW month with the OLD month's still-in-flight rows.
    val byAccount = transactions
        .filter { YearMonth.of(it.occurredAt.date) == month }
        .groupBy { it.accountId }

    return AccountsMonthSlice(
        month = month,
        accounts = accounts.map { account ->
            val own = byAccount[account.accountId].orEmpty()
            val net = own.net()
            AccountMonthUi(
                account = account,
                movementCount = own.size,
                net = net.positiveMoneyFormatted(),
                netIsPositive = net.cents > 0L,
            )
        },
        spent = byAccount.values.flatten().total(TransactionType.Spend).unsigned(),
        income = byAccount.values.flatten().total(TransactionType.Income).unsigned(),
    )
}

private fun List<Transaction>.net(): Money = fold(Money.Zero) { running, transaction ->
    when (transaction.type) {
        TransactionType.Income -> running + transaction.amount
        TransactionType.Spend -> running - transaction.amount
    }
}

private fun List<Transaction>.total(type: TransactionType): Money =
    filter { it.type == type }.fold(Money.Zero) { running, transaction -> running + transaction.amount }

private fun Money.unsigned(): String = formatNeutral(fromCentsToSolesWith(this))
