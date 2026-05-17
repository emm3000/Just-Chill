package com.emm.justchill.hh.shared

import com.emm.domain.shared.Money
import com.emm.justchill.core.format.format

fun fromCentsToSolesWith(money: Money): String = money.format()

private val EmptyString: String = String()

val String.Companion.Empty: String
    get() = EmptyString