package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId

data class FrequentCombo(val accountId: AccountId, val categoryId: CategoryId, val type: TransactionType)
