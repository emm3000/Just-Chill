package com.emm.justchill.core.domain.shared.backup

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.loan.Loan
import com.emm.justchill.core.domain.loan.LoanPayment
import com.emm.justchill.core.domain.recurring.RecurringMovement
import com.emm.justchill.core.domain.transaction.Transaction

// A null list means the Snapshot does not carry that table, so a restore leaves it untouched:
// wiping it would delete rows the Snapshot has no way to put back. An empty list is the opposite,
// a table the Snapshot carries with nothing in it, and a restore does empty the table.
data class LocalSnapshot(
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val recurringMovements: List<RecurringMovement>?,
    val loans: List<Loan>?,
    val loanPayments: List<LoanPayment>?,
)
