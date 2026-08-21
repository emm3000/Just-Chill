package com.emm.domain.loan

import com.emm.domain.shared.Money

const val MIN_INTEREST_BPS: Int = 0
const val MAX_INTEREST_BPS: Int = 10_000

private const val BASIS_POINTS_SCALE: Long = 10_000L
private const val HALF_UP_ROUNDING_OFFSET: Long = BASIS_POINTS_SCALE / 2

// HALF-UP by design, not `NumberFormatEs.integerRounded`'s HALF-EVEN: a display formatter and
// stored money math are different jobs (ADR 010 Decision 3).
fun totalDue(principal: Money, interestBps: Int): Money {
    val interest = (principal.cents * interestBps + HALF_UP_ROUNDING_OFFSET) / BASIS_POINTS_SCALE
    return principal + Money(interest)
}

fun remaining(totalDue: Money, paidSoFar: Money): Money = totalDue - paidSoFar
