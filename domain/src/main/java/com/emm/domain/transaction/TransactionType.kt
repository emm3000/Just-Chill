package com.emm.domain.transaction

enum class TransactionType(val value: String) {

    Income(value = "Ingreso"),
    Spend(value = "Gasto"),
}