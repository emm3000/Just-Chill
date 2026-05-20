package com.emm.justchill.hh.transaction

import com.emm.domain.shared.Money
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val FORMATTER: DecimalFormat = DecimalFormat(
    "#,##0.00",
    DecimalFormatSymbols(Locale.US),
)

internal const val MAX_AMOUNT_DIGITS: Int = 13

private const val CENTS_PER_UNIT = 100.0

internal fun sanitizeCentsInput(raw: String): String = raw.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS)

internal fun formatCentsForDisplay(digits: String): String {
    val cents: Long = if (digits.isEmpty()) 0L else digits.toLong()
    return FORMATTER.format(cents / CENTS_PER_UNIT)
}

internal fun centsToMoney(digits: String): Money {
    val cents: Long = if (digits.isEmpty()) 0L else digits.toLong()
    return Money(cents)
}

internal fun centsToSoles(digits: String): Double {
    val cents: Long = if (digits.isEmpty()) 0L else digits.toLong()
    return cents / CENTS_PER_UNIT
}

internal fun moneyCentsString(money: Money): String = money.cents.toString()
