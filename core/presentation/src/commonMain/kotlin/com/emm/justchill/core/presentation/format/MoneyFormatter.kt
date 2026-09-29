package com.emm.justchill.core.presentation.format

import com.emm.justchill.core.domain.shared.Money

fun Money.format(): String = NumberFormatEs.cents(cents)
