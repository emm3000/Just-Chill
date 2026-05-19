package com.emm.justchill.hh.account

import com.emm.domain.account.Account
import com.emm.justchill.core.mvi.UiIntent

sealed interface AccountsIntent : UiIntent {
    data class OnEditClick(val account: Account) : AccountsIntent
    data class OnEditNameChange(val value: String) : AccountsIntent
    data object OnEditConfirm : AccountsIntent
    data object OnEditDismiss : AccountsIntent

    data class OnDeleteClick(val account: Account) : AccountsIntent
    data object OnDeleteConfirm : AccountsIntent
    data object OnDeleteDismiss : AccountsIntent
}
