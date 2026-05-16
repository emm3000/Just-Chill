package com.emm.justchill.hh.transaction

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val FORMATTER: DecimalFormat = DecimalFormat(
    "#,##0.00",
    DecimalFormatSymbols(Locale.US),
)

internal const val MAX_AMOUNT_DIGITS: Int = 13

/**
 * Normaliza un input arbitrario a una cadena de solo dígitos, capeada a [MAX_AMOUNT_DIGITS].
 * Esta es la forma canónica del state del input de monto.
 */
internal fun sanitizeCentsInput(raw: String): String =
    raw.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS)

/**
 * Formatea una cadena de dígitos (que representa centavos) al display "#,##0.00".
 * Input vacío o "0" devuelven "0.00".
 */
internal fun formatCentsForDisplay(digits: String): String {
    val cents: Long = if (digits.isEmpty()) 0L else digits.toLong()
    return FORMATTER.format(cents / 100.0)
}

/**
 * Convierte una cadena de dígitos (centavos) al monto en soles como Double.
 * Útil para construir el `TransactionInsert`/`TransactionUpdate` del dominio.
 */
internal fun centsToSoles(digits: String): Double {
    val cents: Long = if (digits.isEmpty()) 0L else digits.toLong()
    return cents / 100.0
}

/**
 * Convierte un monto en soles (Double) a la cadena de dígitos canónica.
 * Útil para hidratar el state desde una `Transaction` existente al entrar a Edit.
 */
internal fun solesToCentsString(soles: Double): String {
    val cents: Long = (soles * 100).toLong()
    return cents.toString()
}
