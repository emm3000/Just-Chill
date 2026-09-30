package com.emm.justchill.core.presentation.format

import com.emm.justchill.core.domain.shared.Money

const val CURRENCY_PREFIX: String = "S/\u00A0"

internal const val INCOME_SIGN = "+"

fun formatIncome(value: String): String = "$INCOME_SIGN$CURRENCY_PREFIX$value"

fun formatExpense(value: String): String = "−$CURRENCY_PREFIX$value"

fun formatNeutral(value: String): String = "$CURRENCY_PREFIX$value"

fun Money.balanceFormatted(): String = if (cents < 0L) formatExpense(format()) else formatNeutral(format())

fun Money.positiveMoneyFormatted(): String = if (cents > 0L) formatIncome(format()) else balanceFormatted()
