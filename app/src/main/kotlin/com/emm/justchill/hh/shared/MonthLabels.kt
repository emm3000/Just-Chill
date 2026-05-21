package com.emm.justchill.hh.shared

import com.emm.domain.shared.YearMonth
import kotlinx.datetime.Month

fun YearMonth.fullLabel(): String = "${month.spanish()} $year"

fun YearMonth.shortLabel(): String = month.spanish()

fun YearMonth.shortLabel3(): String = month.spanish().take(3)

fun Month.spanish(): String = when (this) {
    Month.JANUARY -> "Enero"
    Month.FEBRUARY -> "Febrero"
    Month.MARCH -> "Marzo"
    Month.APRIL -> "Abril"
    Month.MAY -> "Mayo"
    Month.JUNE -> "Junio"
    Month.JULY -> "Julio"
    Month.AUGUST -> "Agosto"
    Month.SEPTEMBER -> "Setiembre"
    Month.OCTOBER -> "Octubre"
    Month.NOVEMBER -> "Noviembre"
    Month.DECEMBER -> "Diciembre"
}
