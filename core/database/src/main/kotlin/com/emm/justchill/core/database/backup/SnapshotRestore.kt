package com.emm.justchill.core.database.backup

import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.database.shared.toOccurredAtText
import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.loan.Loan
import com.emm.justchill.core.domain.loan.LoanPayment
import com.emm.justchill.core.domain.recurring.RecurringMovement
import com.emm.justchill.core.domain.transaction.Transaction

// The ledger holds one currency (PRODUCT_REQUIREMENTS.md, soles), and every other write path
// stamps the column with it too; the value never travels with an Account.
private const val LEDGER_CURRENCY: String = "PEN"

internal fun JustChillDatabase.restore(account: Account, now: Long) {
    accountsQueries.insertOrIgnoreFromBackup(
        accountId = account.accountId.value,
        name = account.name,
        type = account.type.name,
        currency = LEDGER_CURRENCY,
        updatedAt = now,
        createdAt = now,
    )
    accountsQueries.restoreFromBackup(
        name = account.name,
        type = account.type.name,
        currency = LEDGER_CURRENCY,
        updatedAt = now,
        accountId = account.accountId.value,
    )
}

internal fun JustChillDatabase.restore(category: Category, now: Long) {
    transactionsQueries.clearCategoryOnTypeChange(
        categoryId = category.categoryId.value,
        categoryType = category.categoryType.name,
    )
    recurring_movementsQueries.clearCategoryOnTypeChange(
        categoryId = category.categoryId.value,
        categoryType = category.categoryType.name,
    )
    categoriesQueries.insertOrIgnoreFromBackup(
        categoryId = category.categoryId.value,
        name = category.name,
        icon = category.icon,
        color = category.color,
        categoryType = category.categoryType.name,
        updatedAt = now,
        createdAt = now,
    )
    categoriesQueries.restoreFromBackup(
        name = category.name,
        icon = category.icon,
        color = category.color,
        categoryType = category.categoryType.name,
        updatedAt = now,
        categoryId = category.categoryId.value,
    )
}

internal fun JustChillDatabase.restore(transaction: Transaction, now: Long) {
    val occurredAt: String = transaction.occurredAt.toOccurredAtText()
    val type: String = transaction.type.name
    val categoryId: String? = usableCategoryId(transaction.categoryId?.value, type)
    transactionsQueries.insertOrIgnoreFromBackup(
        transactionId = transaction.transactionId.value,
        type = type,
        amount = transaction.amount.cents,
        description = transaction.description,
        occurredAt = occurredAt,
        categoryId = categoryId,
        accountId = transaction.accountId.value,
        createdAt = now,
        updatedAt = now,
    )
    transactionsQueries.restoreFromBackup(
        type = type,
        amount = transaction.amount.cents,
        description = transaction.description,
        occurredAt = occurredAt,
        categoryId = categoryId,
        accountId = transaction.accountId.value,
        updatedAt = now,
        transactionId = transaction.transactionId.value,
    )
}

internal fun JustChillDatabase.restore(template: RecurringMovement, now: Long) {
    val type: String = template.type.name
    val categoryId: String? = usableCategoryId(template.categoryId?.value, type)
    recurring_movementsQueries.insertOrIgnoreFromBackup(
        id = template.id.value,
        name = template.name,
        type = type,
        amount = template.amount?.cents,
        description = template.description,
        categoryId = categoryId,
        accountId = template.accountId.value,
        frequency = template.frequency.name,
        dayOfMonth = template.dayOfMonth.toLong(),
        isActive = if (template.isActive) 1L else 0L,
        lastConfirmedPeriod = template.lastConfirmedPeriod,
        createdAt = template.createdAt,
        updatedAt = now,
    )
    recurring_movementsQueries.restoreFromBackup(
        name = template.name,
        type = type,
        amount = template.amount?.cents,
        description = template.description,
        categoryId = categoryId,
        accountId = template.accountId.value,
        frequency = template.frequency.name,
        dayOfMonth = template.dayOfMonth.toLong(),
        isActive = if (template.isActive) 1L else 0L,
        lastConfirmedPeriod = template.lastConfirmedPeriod,
        createdAt = template.createdAt,
        updatedAt = now,
        id = template.id.value,
    )
}

internal fun JustChillDatabase.restore(loan: Loan, now: Long) {
    val lentAt: String = loan.lentAt.toOccurredAtText()
    loansQueries.insertOrIgnoreFromBackup(
        loanId = loan.id.value,
        personName = loan.personName,
        personKey = loan.personKey,
        principal = loan.principal.cents,
        interestBps = loan.interestBps.toLong(),
        totalDue = loan.totalDue.cents,
        note = loan.note,
        lentAt = lentAt,
        createdAt = now,
        updatedAt = now,
    )
    loansQueries.restoreFromBackup(
        personName = loan.personName,
        personKey = loan.personKey,
        principal = loan.principal.cents,
        interestBps = loan.interestBps.toLong(),
        totalDue = loan.totalDue.cents,
        note = loan.note,
        lentAt = lentAt,
        updatedAt = now,
        loanId = loan.id.value,
    )
}

internal fun JustChillDatabase.restore(payment: LoanPayment, now: Long): Boolean {
    if (!holdsLiveLoan(payment.loanId.value)) return false
    val paidAt: String = payment.paidAt.toOccurredAtText()
    loan_paymentsQueries.insertOrIgnoreFromBackup(
        paymentId = payment.id.value,
        loanId = payment.loanId.value,
        amount = payment.amount.cents,
        method = payment.method.name,
        paidAt = paidAt,
        note = payment.note,
        createdAt = now,
        updatedAt = now,
    )
    loan_paymentsQueries.restoreFromBackup(
        loanId = payment.loanId.value,
        amount = payment.amount.cents,
        method = payment.method.name,
        paidAt = paidAt,
        note = payment.note,
        updatedAt = now,
        paymentId = payment.id.value,
    )
    return true
}

// `loan_payments.loanId` is NOT NULL, so a payment whose loan is absent cannot be detached the way
// a transaction's category is — inserting it would abort the whole restore on the foreign key.
internal fun JustChillDatabase.holdsLiveLoan(loanId: String): Boolean =
    loansQueries.byId(loanId).executeAsOneOrNull() != null

internal fun JustChillDatabase.usableCategoryId(categoryId: String?, type: String): String? {
    if (categoryId == null) return null
    val storedType: String? = categoriesQueries.typeOf(categoryId).executeAsOneOrNull()
    return if (storedType == type) categoryId else null
}
