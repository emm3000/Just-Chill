package com.emm.justchill.hh.loan

import com.emm.domain.loan.PersonBalance
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class PersonBalanceUi(val personKey: String, val personName: String, val remaining: String)

// ADR 010: a loan balance is neither income nor spend, so formatIncome/formatExpense here would
// contradict the ADR in the UI layer.
private fun PersonBalance.toUi() = PersonBalanceUi(
    personKey = personKey,
    personName = personName,
    remaining = formatNeutral(fromCentsToSolesWith(remaining)),
)

fun List<PersonBalance>.toUi(): List<PersonBalanceUi> = map { it.toUi() }
