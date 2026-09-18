package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.transaction.TransactionType

data class FrequentComboUi(
    val accountId: String,
    val categoryId: String,
    val type: TransactionType,
    val label: String,
    val colorId: String?,
)
