package com.emm.justchill.core.database.backup

import com.emm.justchill.core.database.JustChillDatabase

// `loan_payments.loanId` is NOT NULL, so a payment whose loan is absent cannot be detached the way
// a transaction's category is — inserting it would abort the whole restore on the foreign key.
internal fun JustChillDatabase.holdsLiveLoan(loanId: String): Boolean =
    loansQueries.byId(loanId).executeAsOneOrNull() != null

internal fun JustChillDatabase.holdsLiveAccount(accountId: String): Boolean =
    accountsQueries.find(accountId).executeAsOneOrNull() != null

internal fun JustChillDatabase.usableCategoryId(categoryId: String?, type: String): String? {
    if (categoryId == null) return null
    val storedType: String? = categoriesQueries.typeOf(categoryId).executeAsOneOrNull()
    return if (storedType == type) categoryId else null
}
