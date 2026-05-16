package com.emm.domain.account

import com.emm.domain.shared.AccountId
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf

class FakeAccountRepository : AccountRepository {

    private val flow = MutableSharedFlow<List<Account>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun send(value: List<Account>) = flow.tryEmit(value)

    override fun all(): Flow<List<Account>> {
        return flowOf()
    }

    override suspend fun find(accountId: AccountId): Account? {
        return flow.firstOrNull()?.get(0)
    }

    override fun default(): Flow<Account?> {
        return flowOf()
    }

    override suspend fun create(account: AccountUpsert) {
    }

    override suspend fun update(accountId: AccountId, account: AccountUpsert) {
    }

    override suspend fun delete(accountId: AccountId) {
    }
}
