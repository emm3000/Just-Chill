package com.emm.justchill.hh.domain

import java.math.BigDecimal

class AmountDbFormatter {

    fun format(value: String): Long {
        val format: String = value.replace(Regex("[^\\d.]"), "")
        val amountBigDecimal = BigDecimal(format)
        return amountBigDecimal.multiply(BigDecimal(100)).toLong()
    }
}