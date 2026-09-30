package com.emm.justchill.feature.report

import com.emm.justchill.core.presentation.format.CURRENCY_PREFIX

internal data class TotalAmountParts(val prefix: String, val integer: String, val decimals: String)

internal fun splitTotalAmount(formatted: String): TotalAmountParts? {
    val dotIndex: Int = formatted.lastIndexOf('.')
    if (!formatted.startsWith(CURRENCY_PREFIX) || dotIndex < CURRENCY_PREFIX.length) return null
    return TotalAmountParts(
        prefix = CURRENCY_PREFIX,
        integer = formatted.substring(CURRENCY_PREFIX.length, dotIndex),
        decimals = formatted.substring(dotIndex),
    )
}
