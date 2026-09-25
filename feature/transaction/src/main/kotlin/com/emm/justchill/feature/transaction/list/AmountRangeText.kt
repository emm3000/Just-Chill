package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.ui.format.balanceFormatted

internal fun Money.balanceFormattedNonBreaking(): String = balanceFormatted().replace(' ', ' ')
