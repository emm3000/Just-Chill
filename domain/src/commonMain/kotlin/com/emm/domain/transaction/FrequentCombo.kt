package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId

data class FrequentCombo(val accountId: AccountId, val categoryId: CategoryId, val type: TransactionType)
