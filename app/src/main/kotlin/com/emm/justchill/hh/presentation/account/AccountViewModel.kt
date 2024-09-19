package com.emm.justchill.hh.presentation.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.domain.account.AccountUpsert
import com.emm.justchill.hh.domain.account.crud.AccountCreator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class AccountViewModel(private val accountCreator: AccountCreator): ViewModel() {

    var name: String by mutableStateOf("")
        private set

    var description: String by mutableStateOf("")
        private set

    var amount by mutableStateOf("")
        private set

    var isEnabled by mutableStateOf(false)
        private set

    init {
        combine(
            snapshotFlow { amount },
            snapshotFlow { name },
        ) { mount, name ->
            isEnabled = mount.isNotEmpty()
                    && name.isNotEmpty()
        }.launchIn(viewModelScope)
    }

    fun updateName(value: String) {
        name = value
    }

    fun updateDescription(value: String) {
        description = value
    }

    fun updateAmount(value: String) {
        amount = value
    }

    fun save() = viewModelScope.launch {
        val accountUpsert = AccountUpsert(
            name = name,
            balance = amount.toDouble(),
            description = description
        )
        accountCreator.create(accountUpsert)
    }
}