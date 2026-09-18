package com.emm.justchill.hh.loan

import com.emm.justchill.core.domain.loan.PersonBalance
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.ui.format.positiveMoneyFormatted

// remainingIsPositive is the raw sign remaining already formats into +/nothing — tone (success vs
// monochrome) lives at the render site, not here.
data class PersonBalanceUi(
    val personKey: String,
    val personName: String,
    val remaining: String,
    val isSettled: Boolean,
    val remainingIsPositive: Boolean,
)

private fun PersonBalance.toUi() = PersonBalanceUi(
    personKey = personKey,
    personName = personName,
    remaining = remaining.positiveMoneyFormatted(),
    isSettled = remaining.cents == 0L,
    remainingIsPositive = remaining.cents > 0L,
)

fun List<PersonBalance>.toUi(): List<PersonBalanceUi> = map { it.toUi() }

// Money owed to the user, so a positive total is signed + and tinted at the call site; zero carries
// no sign, having no direction to point in.
fun List<PersonBalance>.totalOwedFormatted(): String = totalRemaining().positiveMoneyFormatted()

// The aggregate's own sign — never owingNames' emptiness. Two balances can offset to a zero total
// while owingNames still names whoever holds the positive half.
fun List<PersonBalance>.totalOwedIsPositive(): Boolean = totalRemaining().cents > 0L

fun List<PersonBalance>.owingNames(): List<String> = filter { it.remaining.cents > 0L }.map { it.personName }

private fun List<PersonBalance>.totalRemaining(): Money = fold(Money.Zero) { acc, balance -> acc + balance.remaining }
