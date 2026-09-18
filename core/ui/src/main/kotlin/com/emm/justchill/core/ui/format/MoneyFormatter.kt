package com.emm.justchill.core.ui.format

import com.emm.justchill.core.domain.shared.Money

fun Money.format(): String = NumberFormatEs.cents(cents)
