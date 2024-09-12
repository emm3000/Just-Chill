package com.emm.justchill.hh.data.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountModel(

    @SerialName("account_id")
    val accountId: String,

    val name: String,

    val balance: Double,

    val description: String,

    @SerialName("user_id")
    val userId: String = "",
)