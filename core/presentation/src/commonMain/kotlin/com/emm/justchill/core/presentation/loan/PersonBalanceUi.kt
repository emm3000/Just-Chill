package com.emm.justchill.core.presentation.loan

import com.emm.justchill.core.domain.loan.PersonBalance
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.presentation.format.positiveMoneyFormatted

data class PersonBalanceUi(
    val personKey: String,
    val personName: String,
    val remaining: String,
    val isSettled: Boolean,
    val tone: PersonRemainingTone,
)

enum class PersonRemainingTone {
    Muted,
    Positive,
    Neutral,
}

private fun PersonBalance.toUi(): PersonBalanceUi = PersonBalanceUi(
    personKey = personKey,
    personName = personName,
    remaining = remaining.positiveMoneyFormatted(),
    isSettled = remaining.cents == 0L,
    tone = when {
        remaining.cents == 0L -> PersonRemainingTone.Muted
        remaining.cents > 0L -> PersonRemainingTone.Positive
        else -> PersonRemainingTone.Neutral
    },
)

fun List<PersonBalance>.toUi(): List<PersonBalanceUi> = map { it.toUi() }

fun List<PersonBalance>.totalOwedFormatted(): String = totalRemaining().positiveMoneyFormatted()

fun List<PersonBalance>.totalOwedIsPositive(): Boolean = totalRemaining().cents > 0L

fun List<PersonBalance>.owingNames(): List<String> = filter { it.remaining.cents > 0L }.map { it.personName }

private fun List<PersonBalance>.totalRemaining(): Money = fold(Money.Zero) { acc, balance -> acc + balance.remaining }
