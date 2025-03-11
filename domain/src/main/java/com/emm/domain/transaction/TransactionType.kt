package com.emm.domain.transaction

enum class TransactionType(val value: String, val colorHex: String) {

    Income(value = "Ingreso", colorHex = "Color.Blue"),
    Spent(value = "Gasto", colorHex = "Color.Red"),
}