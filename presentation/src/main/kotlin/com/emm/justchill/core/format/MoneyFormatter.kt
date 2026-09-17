package com.emm.justchill.core.format

import com.emm.domain.shared.Money
import com.emm.justchill.hh.shared.NumberFormatEs

fun Money.format(): String = NumberFormatEs.cents(cents)
