package com.emm.justchill.hh.loan

import com.emm.domain.loan.PersonBalance
import com.emm.domain.shared.Money
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

/**
 * Promoted out of `HomeViewModel` (ADR 010): Home and Cuentas each show the total owed for the
 * same balances, and this is the one formatting path both must go through so the two numbers can
 * never drift apart.
 */
fun List<PersonBalance>.totalOwedFormatted(): String = formatNeutral(fromCentsToSolesWith(totalRemaining()))

private fun List<PersonBalance>.totalRemaining(): Money = fold(Money.Zero) { acc, balance -> acc + balance.remaining }
