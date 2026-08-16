package com.emm.domain.auth

import kotlinx.coroutines.flow.Flow

interface ClaimLocalDataRepository {

    suspend fun claimAll(userId: String)

    suspend fun unclaimAll(userId: String)

    fun observeUnclaimedCount(): Flow<Long>
}
