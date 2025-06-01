package com.emm.data.account

class AccountRemoteDataSource {

    suspend fun upsert(account: AccountModel) {}

    suspend fun retrieve(): List<AccountModel> {
        return listOf()
    }

    suspend fun deleteBy(accountId: String) {}

    suspend fun deleteAll() {}
}