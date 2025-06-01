package com.emm.data.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountModel(

    @SerialName("account_id")
    val accountId: String,

    val name: String,

    val balance: Double,

    @SerialName("updated_at")
    val updatedAt: Long,

    @SerialName("user_id")
    val userId: String,
)