package com.emm.justchill.hh.shared

import com.emm.domain.shared.Money

private const val CURRENCY_SYMBOL = "S/"

fun formatIncome(value: String): String = "+$CURRENCY_SYMBOL $value"

fun formatExpense(value: String): String = "−$CURRENCY_SYMBOL $value"

fun formatNeutral(value: String): String = "$CURRENCY_SYMBOL $value"

/**
 * `NumberFormatEs.cents()` (reached through [fromCentsToSolesWith]) applies `abs()` internally, so
 * a [Money] that can be negative must branch on sign here or lose it silently: negative routes
 * through [formatExpense] (sign before the `S/` symbol, DESIGN_SYSTEM.md §3.3), zero and positive
 * share [formatNeutral].
 */
fun Money.balanceFormatted(): String =
    if (cents < 0L) formatExpense(fromCentsToSolesWith(this)) else formatNeutral(fromCentsToSolesWith(this))
