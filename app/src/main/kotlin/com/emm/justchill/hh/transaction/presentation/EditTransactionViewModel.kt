package com.emm.justchill.hh.transaction.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.formatInputToDouble
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountFinder
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.TransactionDeleter
import com.emm.domain.transaction.TransactionFinder
import com.emm.domain.transaction.TransactionUpdater
import com.emm.justchill.hh.transaction.presentation.DateUtils.millisToReadableFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditTransactionViewModel(
    private val transactionId: String,
    private val transactionUpdater: TransactionUpdater,
    private val transactionFinder: TransactionFinder,
    private val transactionDeleter: TransactionDeleter,
    private val accountFinder: AccountFinder,
    accountRepository: AccountRepository,
) : ViewModel() {

    var accountSelected: Account? by mutableStateOf(null)
        private set

    var amount by mutableStateOf(TextFieldValue("0.00"))
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var isEnabled by mutableStateOf(false)
        private set

    var transactionType by mutableStateOf(TransactionType.Income)
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.retrieve()
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
        ) { mount, date, description ->
            isEnabled = mount.formatInputToDouble() >= 1
                    && date.isNotEmpty()
                    && description.isNotEmpty()
        }.launchIn(viewModelScope)

        loadCurrentTransaction()
    }

    private fun loadCurrentTransaction() = viewModelScope.launch {
        val currentTransaction = transactionFinder.find(transactionId).firstOrNull()
        currentTransaction?.let { transaction ->
            amount = TextFieldValue(transaction.amountDecimalFormat)
            description = transaction.description
            transactionType = TransactionType.valueOf(transaction.type)
            date = millisToReadableFormat(transaction.date)
            dateInLong = transaction.date
            val account: Account? = accountFinder.find(currentTransaction.accountId).firstOrNull()
            account?.let {
                accountSelected = it
            }
        }
    }

    fun updateTransaction() = viewModelScope.launch {
        val transactionUpdate = TransactionUpdate(
            type = transactionType,
            description = description,
            date = dateInLong,
            amount = amount.formatInputToDouble(),
            accountId = accountSelected?.accountId ?: throw IllegalStateException()
        )
        transactionUpdater.update(transactionId, transactionUpdate)
    }

    fun deleteTransaction() = viewModelScope.launch {
        transactionDeleter.delete(transactionId)
    }

    fun updateMount(value: TextFieldValue) {
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