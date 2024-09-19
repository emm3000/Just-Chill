package com.emm.justchill.hh.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.domain.account.Account
import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.domain.account.crud.AccountFinder
import com.emm.justchill.hh.domain.transaction.model.TransactionUpdate
import com.emm.justchill.hh.domain.transaction.crud.TransactionDeleter
import com.emm.justchill.hh.domain.transaction.crud.TransactionFinder
import com.emm.justchill.hh.domain.transaction.crud.TransactionUpdater
import com.emm.justchill.hh.presentation.transaction.DateUtils.millisToReadableFormat
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

    var amount by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var isEnabled by mutableStateOf(false)
        private set

    var transactionType by mutableStateOf(TransactionType.INCOME)
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
        ) { mount, date, description ->
            isEnabled = mount.isNotEmpty()
                    && date.isNotEmpty()
                    && description.isNotEmpty()
        }.launchIn(viewModelScope)

        loadCurrentTransaction()
    }

    private fun loadCurrentTransaction() = viewModelScope.launch {
        val currentTransaction = transactionFinder.find(transactionId).firstOrNull()
        currentTransaction?.let { transaction ->
            amount = transaction.amount.toString()
            description = transaction.description
            transactionType = TransactionType.valueOf(transaction.type)
            date = millisToReadableFormat(transaction.date)
            dateInLong = transaction.date
            val account: Account? = accountFinder.find(currentTransaction.accountId).firstOrNull()
            account?.let {
                accountLabel = "${it.name} ${it.balance}"
                accountSelected = it
            }
        }
    }

    fun updateTransaction() = viewModelScope.launch {
        val transactionUpdate = TransactionUpdate(
            type = transactionType.name,
            description = description,
            date = dateInLong,
            amount = amount.replace(Regex("[^\\d.]"), "").toDouble(),
            accountId = accountSelected?.accountId ?: throw IllegalStateException()
        )
        transactionUpdater.update(transactionId, transactionUpdate)
    }

    fun deleteTransaction() = viewModelScope.launch {
        transactionDeleter.delete(transactionId)
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