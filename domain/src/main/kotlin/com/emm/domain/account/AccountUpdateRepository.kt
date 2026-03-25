package com.emm.domain.account

interface AccountUpdateRepository {

    suspend fun update(accountId: String, account: AccountUpsert)
}
