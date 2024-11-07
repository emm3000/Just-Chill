package com.emm.justchill.hh.home

import androidx.compose.runtime.Immutable
import com.emm.justchill.hh.account.domain.Account

@Immutable
data class HomeState(
    val accounts: List<Account> = emptyList(),
    val accountSelected: Account? = null,
    val difference: String = "",
    val income: String = "",
    val spend: String = "",
)