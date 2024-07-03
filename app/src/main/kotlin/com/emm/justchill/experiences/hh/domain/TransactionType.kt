package com.emm.justchill.experiences.hh.domain

import androidx.compose.ui.graphics.Color

enum class TransactionType(val value: String, val color: Color) {

    INCOME(value = "Ingreso", color = Color.Blue),
    SPENT(value = "Gasto", color = Color.Red),
}