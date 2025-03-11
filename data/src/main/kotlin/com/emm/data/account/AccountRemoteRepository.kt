package com.emm.data.account

interface AccountRemoteRepository {

    suspend fun upsert(account: AccountModel)

    suspend fun retrieve(): List<AccountModel>

    suspend fun deleteBy(accountId: String)

    suspend fun deleteAll()
}