package com.emm.data.category

import com.emm.data.auth.UserIdProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryRemoteDataSource(
    userIdProvider: UserIdProvider,
) : UserIdProvider by userIdProvider {

    suspend fun upsert(categories: List<NetworkCategory>) = withContext(Dispatchers.IO) {
    }

    suspend fun all(): List<NetworkCategory> = listOf()
}
