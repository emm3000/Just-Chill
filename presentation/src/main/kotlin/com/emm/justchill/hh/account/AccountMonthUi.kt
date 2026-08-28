package com.emm.justchill.hh.account

import com.emm.domain.account.Account
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.balanceFormatted
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith

/**
 * An account has no opening balance, so [net] is a monthly net and never a balance — the screen owes
 * the user the "este mes" caption that says so.
 */
data class AccountMonthUi(val account: Account, val movementCount: Int, val net: String)

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
    // The month a movement belongs to is the one it carries. No zone, no conversion, nothing that
    // can put the same movement in a different month on a different device.
    val byAccount = transactions
        .filter { YearMonth.of(it.occurredAt.date) == month }
        .groupBy { it.accountId }

    return AccountsMonthSlice(
        month = month,
        accounts = accounts.map { account ->
            val own = byAccount[account.accountId].orEmpty()
            AccountMonthUi(account = account, movementCount = own.size, net = own.net().balanceFormatted())
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
