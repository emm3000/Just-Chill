package com.emm.justchill.hh.account.domain

import com.emm.justchill.hh.shared.UniqueIdProvider
import kotlinx.coroutines.flow.firstOrNull

class DailyAccountCreator(
    private val repository: AccountRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(): String {
        val existDaily: Account? = repository.existDailyAccount().firstOrNull()

        if (existDaily != null) return existDaily.accountId

        val uniqueId: String = uniqueIdProvider.uniqueId
        val accountUpsert = AccountUpsert(
            name = "FERIA",
            balance = 0.0,
            description = "JUST FERIA"
        )
        repository.create(uniqueId, accountUpsert)
        return uniqueId
    }
}