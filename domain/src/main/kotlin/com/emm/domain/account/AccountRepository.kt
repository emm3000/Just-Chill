package com.emm.domain.account

import com.emm.domain.shared.AccountId
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    fun all(): Flow<List<Account>>

    suspend fun find(accountId: AccountId): Account?

    fun default(): Flow<Account?>

    suspend fun create(account: AccountUpsert)

    suspend fun update(accountId: AccountId, account: AccountUpsert)

    suspend fun delete(accountId: AccountId)

    suspend fun pull()

    suspend fun sync()
}