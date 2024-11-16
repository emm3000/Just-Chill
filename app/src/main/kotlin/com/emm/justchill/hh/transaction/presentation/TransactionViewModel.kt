package com.emm.justchill.hh.transaction.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.formatInputToDouble
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.account.domain.AccountRepository
import com.emm.justchill.hh.transaction.domain.TransactionCreator
import com.emm.justchill.hh.transaction.domain.TransactionInsert
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionCreator: TransactionCreator,
    accountRepository: AccountRepository,
) : ViewModel() {

    var amount by mutableStateOf(TextFieldValue("0.00"))
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var transactionType: TransactionType by mutableStateOf(TransactionType.INCOME)
        private set

    var isEnabled by mutableStateOf(false)
        private set

    var accountSelected: Account? by mutableStateOf(null)
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.retrieve()
        .onEach { accounts ->
            accountSelected = accountSelected ?: accounts.firstOrNull()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        combine(
            snapshotFlow { amount },
            snapshotFlow { date },
            snapshotFlow { description },
            snapshotFlow { accountSelected },
        ) { mount, date, description, account ->
            isEnabled = mount.formatInputToDouble() >= 1.0
                    && date.isNotEmpty()
                    && description.isNotEmpty()
                    && account != null
        }.launchIn(viewModelScope)
    }

    fun addTransaction() = viewModelScope.launch {
        val transactionInsert = TransactionInsert(
            type = transactionType,
            description = description,
            date = dateInLong,
            amount = amount.formatInputToDouble(),
            accountId = accountSelected?.accountId ?: throw IllegalStateException()
        )
        transactionCreator.create(transactionInsert)
    }

    fun updateAmount(value: TextFieldValue) {
        amount = value
    }

    fun updateDescription(value: String) {
        description = value
    }

    fun updateCurrentDate(millis: Long?) {
        if (millis != null) {
            dateInLong = millis
            date = DateUtils.millisToReadableFormatUTC(millis)
        }
    }

    fun updateTransactionType(value: TransactionType) {
        transactionType = value
    }

    fun updateAccountSelected(value: Account) {
        accountSelected = value
    }
}