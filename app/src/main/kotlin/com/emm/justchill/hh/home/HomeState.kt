package com.emm.justchill.hh.home

import androidx.compose.runtime.Immutable

@Immutable
data class HomeState(
    val income: String = "",
    val spend: String = "",
)