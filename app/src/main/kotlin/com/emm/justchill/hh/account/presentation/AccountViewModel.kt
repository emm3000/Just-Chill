package com.emm.justchill.hh.account.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.formatInputToDouble
import com.emm.justchill.hh.account.domain.AccountUpsert
import com.emm.justchill.hh.account.domain.AccountCreator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class AccountViewModel(private val accountCreator: AccountCreator): ViewModel() {

    var name: String by mutableStateOf("")
        private set

    var description: String by mutableStateOf("")
        private set

    var amount by mutableStateOf(TextFieldValue("0.00"))
        private set

    var isEnabled by mutableStateOf(false)
        private set

    init {
        combine(
            snapshotFlow { amount },
            snapshotFlow { name },
        ) { mount, name ->
            isEnabled = mount.text.isNotEmpty()
                    && name.isNotEmpty()
        }.launchIn(viewModelScope)
    }

    fun updateName(value: String) {
        name = value
    }

    fun updateDescription(value: String) {
        description = value
    }

    fun updateAmount(value: TextFieldValue) {
        amount = value
    }

    fun save() = viewModelScope.launch {
        val accountUpsert = AccountUpsert(
            name = name,
            balance = amount.formatInputToDouble(),
            description = description
        )
        accountCreator.create(accountUpsert)
    }
}