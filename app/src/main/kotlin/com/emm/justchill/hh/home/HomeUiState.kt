package com.emm.justchill.hh.home

import androidx.compose.runtime.Immutable
import com.emm.domain.account.Account

@Immutable
data class HomeUiState(
    val income: String = "",
    val spend: String = "",
    val account: Account? = null,
)