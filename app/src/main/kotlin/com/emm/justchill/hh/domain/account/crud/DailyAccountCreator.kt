package com.emm.justchill.hh.domain.account.crud

import com.emm.justchill.hh.domain.account.Account
import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.domain.account.AccountUpsert
import com.emm.justchill.hh.domain.shared.UniqueIdProvider
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