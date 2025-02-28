package com.emm.domain.transaction

enum class TransactionType(val value: String, val colorHex: String) {

    INCOME(value = "Ingreso", colorHex = "Color.Blue"),
    SPENT(value = "Gasto", colorHex = "Color.Red"),
}