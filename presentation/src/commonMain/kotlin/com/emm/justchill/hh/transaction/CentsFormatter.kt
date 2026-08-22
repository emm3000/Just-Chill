package com.emm.justchill.hh.transaction

import com.emm.domain.shared.Money
import com.emm.justchill.hh.shared.NumberFormatEs

const val MAX_AMOUNT_DIGITS: Int = 13

private const val CENTS_PER_UNIT = 100.0

fun sanitizeCentsInput(raw: String): String = raw.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS)

fun formatCentsForDisplay(digits: String): String =
    NumberFormatEs.cents(if (digits.isEmpty()) 0L else digits.toLong())

fun centsToMoney(digits: String): Money {
    val cents: Long = if (digits.isEmpty()) 0L else digits.toLong()
    return Money(cents)
}

fun String.isSavableAmount(): Boolean = isNotEmpty() && toLongOrNull() != 0L

fun centsToSoles(digits: String): Double {
    val cents: Long = if (digits.isEmpty()) 0L else digits.toLong()
    return cents / CENTS_PER_UNIT
}

fun moneyCentsString(money: Money): String = money.cents.toString()
