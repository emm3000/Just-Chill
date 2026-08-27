package com.emm.domain.shared

import kotlin.jvm.JvmInline

@JvmInline
value class Money(val cents: Long) {

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money(cents - other.cents)

    operator fun unaryMinus(): Money = Money(-cents)

    companion object {
        val Zero: Money = Money(0L)
    }
}
