package com.emm.justchill.hh.transaction

import com.emm.domain.transaction.TransactionType

/**
 * UI model for a frequent account+category+type combo.
 * [colorId] is the category's palette id (domain string); the Compose rendering site resolves it
 * via `findById(colorId).primary`. Null when the combo's category carries no color.
 */
data class FrequentComboUi(
    val accountId: String,
    val categoryId: String,
    val type: TransactionType,
    val label: String,
    val colorId: String?,
)
