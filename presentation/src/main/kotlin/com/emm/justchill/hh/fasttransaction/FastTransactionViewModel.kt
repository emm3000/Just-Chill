package com.emm.justchill.hh.fasttransaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.formatInputToDouble
import com.emm.justchill.hh.transaction.domain.TransactionCreator
import com.emm.justchill.hh.transaction.domain.TransactionInsert
import com.emm.justchill.hh.transaction.presentation.DateUtils
import com.emm.justchill.hh.transaction.presentation.TransactionType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FastTransactionViewModel(
    private val transactionCreator: TransactionCreator,
) : ViewModel() {

    var amount by mutableStateOf(TextFieldValue("0.00"))
        private set

    var description by mutableStateOf("")
        private set

    var isEnabled by mutableStateOf(false)
        private set

    init {
        snapshotFlow { amount }
            .onEach {
                isEnabled = amount.formatInputToDouble() > 1.0
            }
            .launchIn(viewModelScope)
    }

    fun addTransaction(accountId: String, type: TransactionType) {
        viewModelScope.launch {
            transactionCreator.create(
                TransactionInsert(
                    type = type,
                    amount = amount.formatInputToDouble(),
                    description = description,
                    date = DateUtils.currentDateInMillis(),
                    accountId = accountId
                )
            )
        }
    }

    fun updateAmount(value: TextFieldValue) {
        amount = value
    }

    fun updateDescription(value: String) {
        description = value
    }
}