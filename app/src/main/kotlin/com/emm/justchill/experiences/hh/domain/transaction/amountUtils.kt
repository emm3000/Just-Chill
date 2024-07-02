package com.emm.justchill.experiences.hh.domain.transaction

import java.math.BigDecimal
import java.math.RoundingMode

fun fromCentsToSoles(cents: Long): BigDecimal {
    return BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
}