package com.emm.justchill.hh.domain.account

import com.emm.justchill.hh.domain.shared.fromCentsToSolesWith

data class Account(
    val accountId: String,
    val name: String,
    val balance: Double,
    val description: String,
    val syncStatus: String,
) {

    val nameWithBalance: String
        get() = "$name - ${fromCentsToSolesWith(balance)}"
}