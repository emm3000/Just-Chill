package com.emm.domain.loan

import com.emm.domain.shared.Money

data class PersonBalance(val personKey: String, val personName: String, val remaining: Money)
