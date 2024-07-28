package com.emm.justchill.loans.domain

enum class FrequencyType(val value: String, val days: Int) {

    DAILY(value = "Diario", days = 1),
    WEEKLY(value = "Semanal", days = 7),
    MONTHLY(value = "Mensual", days = 30),
}