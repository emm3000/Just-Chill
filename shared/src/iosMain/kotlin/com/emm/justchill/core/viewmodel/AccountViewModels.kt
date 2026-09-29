package com.emm.justchill.core.viewmodel

import com.emm.justchill.feature.account.AccountsEffect
import com.emm.justchill.feature.account.AccountsIntent
import com.emm.justchill.feature.account.AccountsUiState
import com.emm.justchill.feature.account.AccountsViewModel
import com.emm.justchill.feature.account.AddAccountEffect
import com.emm.justchill.feature.account.AddAccountIntent
import com.emm.justchill.feature.account.AddAccountUiState
import com.emm.justchill.feature.account.AddAccountViewModel

fun accountsViewModel(): MviHandle<AccountsUiState, AccountsIntent, AccountsEffect> = handleOf(AccountsViewModel::class)

fun addAccountViewModel(): MviHandle<AddAccountUiState, AddAccountIntent, AddAccountEffect> =
    handleOf(AddAccountViewModel::class)
