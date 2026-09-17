package com.emm.justchill.core.domain.loan

import com.emm.justchill.core.domain.shared.Money

data class PersonBalance(val personKey: String, val personName: String, val remaining: Money)
