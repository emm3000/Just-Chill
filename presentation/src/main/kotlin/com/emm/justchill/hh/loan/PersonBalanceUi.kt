package com.emm.justchill.hh.loan

import com.emm.domain.loan.PersonBalance
import com.emm.domain.shared.Money
import com.emm.justchill.hh.shared.formatIncome
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
 * Money owed to the user, so a non-zero total is signed `+` and tinted `success` at the call site
 * (DESIGN_SYSTEM.md §1.4/§3.3); zero carries no sign, having no direction to point in.
 */
fun List<PersonBalance>.totalOwedFormatted(): String {
    val total = totalRemaining()
    val amount = fromCentsToSolesWith(total)
    return if (total.cents > 0L) formatIncome(amount) else formatNeutral(amount)
}

fun List<PersonBalance>.owingNames(): List<String> = filter { it.remaining.cents > 0L }.map { it.personName }

private fun List<PersonBalance>.totalRemaining(): Money = fold(Money.Zero) { acc, balance -> acc + balance.remaining }
