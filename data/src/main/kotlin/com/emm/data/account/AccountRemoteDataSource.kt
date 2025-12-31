package com.emm.data.account

import com.emm.data.auth.UserIdProvider

class AccountRemoteDataSource(
    userIdProvider: UserIdProvider,
) : UserIdProvider by userIdProvider {

    suspend fun upsert(accounts: List<AccountModel>) {

    }

    private fun attachUserIdToCategory(accountModel: AccountModel) = accountModel.copy(userId = userId)

    suspend fun all(): List<AccountModel> = listOf()

    suspend fun delete(accountId: String) {
    }
}