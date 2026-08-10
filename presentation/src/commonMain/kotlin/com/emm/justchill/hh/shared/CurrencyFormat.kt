package com.emm.justchill.hh.shared

private const val CURRENCY_SYMBOL = "S/"

fun formatIncome(value: String): String = "+$CURRENCY_SYMBOL $value"

fun formatExpense(value: String): String = "−$CURRENCY_SYMBOL $value"

fun formatNeutral(value: String): String = "$CURRENCY_SYMBOL $value"
