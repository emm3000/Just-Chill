package com.emm.justchill.hh.transaction

import com.emm.domain.transaction.TransactionType

/**
 * UI model for a frequent account+category+type combo.
 * [dotColor] is stored as ARGB Long so this class remains usable in JVM unit tests
 * (androidx.compose.ui.graphics.Color is Android-only and absent from the JVM test classpath).
 * Convert to Color at the Compose rendering site via Color(dotColor ?: 0L).
 */
data class FrequentComboUi(
    val accountId: String,
    val categoryId: String,
    val type: TransactionType,
    val label: String,
    val dotColor: Long?,
)
