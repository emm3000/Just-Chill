package com.emm.justchill.hh.transaction

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.FindAccountUseCase
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.FindTransactionUseCase
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.DateUtils.millisToReadableFormat
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class EditTransactionViewModel(
    private val transactionId: String,
    private val accountRepository: AccountRepository,
    private val updateTransaction: UpdateTransactionUseCase,
    private val findTransaction: FindTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val findAccount: FindAccountUseCase,
) : MviViewModel<EditTransactionUiState, EditTransactionIntent, EditTransactionEffect>() {

    override val initialState = EditTransactionUiState()

    private var oldAccount: Account = Account.Empty
    private var oldTransaction: Transaction = Transaction.Empty
    private var dateInLong: Long = DateUtils.currentDateInMillis()

    init {
        loadCurrentTransaction()
    }

    override fun onIntent(intent: EditTransactionIntent) {
        when (intent) {
            is EditTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value).recomputeValidity() }
            is EditTransactionIntent.OnDateChange -> updateState { copy(date = intent.value) }
            is EditTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value).recomputeValidity() }
            is EditTransactionIntent.OnTransactionTypeChange -> updateState { copy(transactionType = intent.value) }
            is EditTransactionIntent.OnDateChangeInMillis -> updateCurrentDate(intent.value)
            is EditTransactionIntent.OnAccountSelected -> updateState { copy(accountSelected = intent.value) }
            EditTransactionIntent.OnSave -> updateTransaction()
            EditTransactionIntent.OnDelete -> deleteTransaction()
        }
    }

    private fun EditTransactionUiState.recomputeValidity(): EditTransactionUiState =
        copy(isEnabled = centsToSoles(amount) >= 1.0 && date.isNotEmpty() && description.isNotEmpty())

    private fun loadCurrentTransaction() = viewModelScope.launch {
        val accounts: List<Account> = accountRepository.all().firstOrNull() ?: emptyList()
        oldTransaction = findTransaction(TransactionId(transactionId)) ?: return@launch
        oldAccount = findAccount(oldTransaction.accountId) ?: return@launch
        dateInLong = oldTransaction.date
        updateState {
            copy(
                amount = solesToCentsString(oldTransaction.amount),
                description = oldTransaction.description,
                date = millisToReadableFormat(oldTransaction.date),
                transactionType = oldTransaction.type,
                accounts = accounts,
                accountSelected = oldAccount,
            ).recomputeValidity()
        }
    }

    private fun updateTransaction() = launchSafe(
        onError = { EditTransactionEffect.ShowError(it.toUserMessage()) },
    ) {
        updateTransaction(oldTransaction, createTransactionUpdate())
        sendEffect(EditTransactionEffect.TransactionUpdated)
    }

    private fun createTransactionUpdate(): TransactionUpdate = TransactionUpdate(
        type = currentState.transactionType,
        description = currentState.description,
        date = dateInLong,
        amount = centsToSoles(currentState.amount),
        accountId = currentState.accountSelected?.accountId ?: throw IllegalStateException(),
        categoryId = null,
    )

    private fun deleteTransaction() = launchSafe(
        onError = { EditTransactionEffect.ShowError(it.toUserMessage()) },
    ) {
        deleteTransaction(oldTransaction.transactionId)
        sendEffect(EditTransactionEffect.TransactionDeleted)
    }

    private fun updateCurrentDate(millis: Long?) = millis?.let {
        dateInLong = it
        updateState { copy(date = DateUtils.millisToReadableFormatUTC(it)) }
    }
}
