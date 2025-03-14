package com.emm.domain.account

interface AccountUpdateRepository {

    suspend fun update(accountId: String, account: AccountUpsert)

    suspend fun updateAmount(accountId: String, amount: Double)

    suspend fun updateSelected(accountId: String)
}