package com.emm.justchill.core.ui.format

import com.emm.justchill.core.domain.shared.Money

private const val CURRENCY_SYMBOL = "S/"

internal const val INCOME_SIGN = "+"

fun formatIncome(value: String): String = "$INCOME_SIGN$CURRENCY_SYMBOL $value"

fun formatExpense(value: String): String = "−$CURRENCY_SYMBOL $value"

fun formatNeutral(value: String): String = "$CURRENCY_SYMBOL $value"

// NumberFormatEs.cents() applies abs() internally, so a Money that can be negative must branch on
// sign here or lose it silently.
fun Money.balanceFormatted(): String = if (cents < 0L) formatExpense(format()) else formatNeutral(format())

// A positive net or balance aggregate is signed + here so the call site can tint it success; zero
// and negative fall through to balanceFormatted unchanged.
fun Money.positiveMoneyFormatted(): String = if (cents > 0L) formatIncome(format()) else balanceFormatted()
