package com.emm.domain.account

import com.emm.domain.shared.UniqueIdProvider
import kotlinx.coroutines.flow.firstOrNull

class DailyAccountCreator(
    private val repository: AccountRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(): String {
        val existDaily: Account? = null

        if (existDaily != null) return existDaily.accountId

        val uniqueId: String = uniqueIdProvider.id
        val accountUpsert = AccountUpsert(
            name = "FERIA",
            balance = 0.0,
            description = "JUST FERIA",
            isSelected = AccountSelect.NonSelected,
        )
        repository.create(accountUpsert)
        return uniqueId
    }
}