package com.emm.justchill.hh.loan

import com.emm.domain.loan.PersonBalance
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class PersonBalanceUi(val personKey: String, val personName: String, val remaining: String, val isSettled: Boolean)

private fun PersonBalance.toUi() = PersonBalanceUi(
    personKey = personKey,
    personName = personName,
    remaining = formatNeutral(fromCentsToSolesWith(remaining)),
    isSettled = remaining.cents == 0L,
)

fun List<PersonBalance>.toUi(): List<PersonBalanceUi> = map { it.toUi() }
