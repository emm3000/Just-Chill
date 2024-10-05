package com.emm.justchill.hh.account.data

interface AccountRemoteRepository {

    suspend fun upsert(account: AccountModel)

    suspend fun retrieve(): List<AccountModel>

    suspend fun deleteBy(accountId: String)

    suspend fun deleteAll()
}