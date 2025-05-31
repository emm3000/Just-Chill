package com.emm.domain.account

import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    fun all(): Flow<List<Account>>

    suspend fun find(accountId: String): Account?

    fun default(): Flow<Account?>

    suspend fun create(account: AccountUpsert)

    suspend fun deleteBy(accountId: String)
}