package com.emm.domain.transaction

import com.emm.domain.account.Account
import com.emm.domain.shared.SyncState

data class TransactionUpdate(
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val account: Account,
    val date: Long,
    val syncState: SyncState = SyncState.Pending,
)