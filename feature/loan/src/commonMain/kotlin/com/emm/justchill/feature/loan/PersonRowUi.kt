package com.emm.justchill.feature.loan

import com.emm.justchill.core.presentation.loan.PersonBalanceUi

data class PersonRowUi(
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

private fun PersonBalanceUi.toRow(): PersonRowUi = PersonRowUi(
    personKey = personKey,
    personName = personName,
    remaining = remaining,
    isSettled = isSettled,
    tone = when {
        isSettled -> PersonRemainingTone.Muted
        remainingIsPositive -> PersonRemainingTone.Positive
        else -> PersonRemainingTone.Neutral
    },
)

internal fun List<PersonBalanceUi>.toRows(): List<PersonRowUi> = map { it.toRow() }
