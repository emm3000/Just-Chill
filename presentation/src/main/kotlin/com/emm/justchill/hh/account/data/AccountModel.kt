package com.emm.justchill.hh.account.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountModel(

    @SerialName("account_id")
    val accountId: String,

    val name: String,

    val balance: Double,

    @SerialName("initial_balance")
    val initialBalance: Double,

    val description: String,

    @SerialName("user_id")
    val userId: String = "",
)