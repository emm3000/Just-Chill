package com.emm.domain.transaction

enum class TransactionType(val label: String) {

    Income(label = "Ingreso"),
    Spend(label = "Gasto"),
}