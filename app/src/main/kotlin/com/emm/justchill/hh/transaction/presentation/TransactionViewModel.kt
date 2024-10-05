package com.emm.justchill.hh.transaction.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.account.domain.AccountRepository
import com.emm.justchill.hh.transaction.domain.TransactionInsert
import com.emm.justchill.hh.transaction.domain.TransactionCreator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionCreator: TransactionCreator,
    accountRepository: AccountRepository
) : ViewModel() {

    var amount by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var isEnabled: Boolean by mutableStateOf(false)
        private set

    var transactionType: TransactionType by mutableStateOf(TransactionType.INCOME)
        private set

    var accountLabel: String by mutableStateOf("")
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.retrieve()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private var accountSelected: Account? = null

    init {
        combine(
            snapshotFlow { amount },
            snapshotFlow { date },
            snapshotFlow { description },
            snapshotFlow { accountLabel },
        ) { mount, date, description, accountLabel ->
            isEnabled = mount.isNotEmpty()
                    && date.isNotEmpty()
                    && description.isNotEmpty()
                    && accountLabel.isNotEmpty()
        }.launchIn(viewModelScope)
    }

    fun addTransaction() = viewModelScope.launch {
        val transactionInsert = TransactionInsert(
            type = transactionType,
            description = description,
            date = dateInLong,
            amount = amount.toDouble(),
            accountId = accountSelected?.accountId ?: throw IllegalStateException()
        )
        transactionCreator.create(transactionInsert)
    }

    fun updateMount(value: String) {
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

    fun updateAccountLabel(value: String) {
        accountLabel = value
    }

    fun updateAccountSelected(value: Account) {
        accountSelected = value
    }
}