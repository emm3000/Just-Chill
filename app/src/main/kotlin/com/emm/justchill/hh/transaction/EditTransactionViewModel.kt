package com.emm.justchill.hh.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.Account
import com.emm.domain.account.FindAccountUseCase
import com.emm.domain.account.AccountRepository
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.FindTransactionUseCase
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.core.formatInputToDouble
import com.emm.justchill.hh.transaction.DateUtils.millisToReadableFormat
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class EditTransactionViewModel(
    private val transactionId: String,
    private val accountRepository: AccountRepository,
    private val transactionUpdater: UpdateTransactionUseCase,
    private val transactionFinder: FindTransactionUseCase,
    private val transactionDeleter: DeleteTransactionUseCase,
    private val accountFinder: FindAccountUseCase,
) : ViewModel() {

    var state by mutableStateOf(AddTransactionUiState())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    private var oldAccount: Account = Account.Empty

    private var oldTransaction: Transaction = Transaction.Empty

    init {
        combine(
            snapshotFlow { state.amount },
            snapshotFlow { state.date },
            snapshotFlow { state.description },
        ) { mount, date, description ->
            val isEnabled = mount.formatInputToDouble() >= 1
                    && date.isNotEmpty()
                    && description.isNotEmpty()
            state = state.copy(isEnabled = isEnabled)
        }.launchIn(viewModelScope)
        loadCurrentTransaction()
    }

    fun onAction(action: AddTransactionAction) {
        when (action) {
            is AddTransactionAction.OnAmountChange -> state = state.copy(amount = action.value)
            is AddTransactionAction.OnDateChange -> state = state.copy(date = action.value)
            is AddTransactionAction.OnDescriptionChange -> state = state.copy(description = action.value)
            is AddTransactionAction.OnTransactionTypeChange -> state = state.copy(transactionType = action.value)
            is AddTransactionAction.OnDateChangeInMillis -> updateCurrentDate(action.value)
            is AddTransactionAction.OnAccountSelected -> state = state.copy(accountSelected = action.value)
            AddTransactionAction.OnSave -> updateTransaction()
            AddTransactionAction.OnReset -> {}
            AddTransactionAction.OnDelete -> deleteTransaction()
            else -> {}
        }
    }

    private fun loadCurrentTransaction() = viewModelScope.launch {
        val accounts: List<Account> = accountRepository.all().firstOrNull() ?: emptyList()
        oldTransaction = transactionFinder(transactionId) ?: return@launch
        oldAccount = accountFinder(oldTransaction.accountId) ?: return@launch
        state = configInitialState(oldTransaction, oldAccount, accounts)
        dateInLong = oldTransaction.date
    }

    private fun configInitialState(
        currentTransaction: Transaction,
        account: Account,
        accounts: List<Account>,
    ): AddTransactionUiState = state.copy(
        amount = TextFieldValue(currentTransaction.amountDecimalFormat),
        description = currentTransaction.description,
        date = millisToReadableFormat(currentTransaction.date),
        transactionType = currentTransaction.type,
        accounts = accounts,
        accountSelected = account,
    )

    private fun updateTransaction() = viewModelScope.launch {
        val transactionUpdate: TransactionUpdate = createTransactionUpdate()
        transactionUpdater(oldTransaction, transactionUpdate)
    }

    private fun createTransactionUpdate() = TransactionUpdate(
        type = state.transactionType,
        description = state.description,
        date = dateInLong,
        amount = state.amount.formatInputToDouble(),
        accountId = state.accountSelected?.accountId ?: throw IllegalStateException(),
        categoryId = null,
    )

    private fun deleteTransaction() = viewModelScope.launch {
        transactionDeleter(oldTransaction.transactionId)
    }

    private fun updateCurrentDate(millis: Long?) = millis?.let {
        dateInLong = it
        state = state.copy(date = DateUtils.millisToReadableFormatUTC(it))
    }
}
