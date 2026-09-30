package com.emm.justchill.core.presentation.format

import com.emm.justchill.core.domain.shared.Money

private const val CURRENCY_SYMBOL = "S/\u00A0"

internal const val INCOME_SIGN = "+"

fun formatIncome(value: String): String = "$INCOME_SIGN$CURRENCY_SYMBOL$value"

fun formatExpense(value: String): String = "−$CURRENCY_SYMBOL$value"

fun formatNeutral(value: String): String = "$CURRENCY_SYMBOL$value"

// NumberFormatEs.cents() applies abs() internally, so a Money that can be negative must branch on
// sign here or lose it silently.
fun Money.balanceFormatted(): String = if (cents < 0L) formatExpense(format()) else formatNeutral(format())

fun Money.positiveMoneyFormatted(): String = if (cents > 0L) formatIncome(format()) else balanceFormatted()
