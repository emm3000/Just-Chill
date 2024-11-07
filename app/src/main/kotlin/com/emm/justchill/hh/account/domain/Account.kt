package com.emm.justchill.hh.account.domain

import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class Account(
    val accountId: String,
    val name: String,
    val balance: Double,
    val initialBalance: Double,
    val description: String,
    val syncStatus: String,
) {

    val nameWithBalance: String
        get() = "$name - S/ ${fromCentsToSolesWith(balance)}"

    override fun toString(): String = name
}