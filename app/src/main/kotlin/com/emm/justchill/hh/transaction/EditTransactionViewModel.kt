package com.emm.justchill.hh.transaction

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.FindAccountUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.FindTransactionUseCase
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.formatInputToDouble
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.DateUtils.millisToReadableFormat
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class EditTransactionViewModel(
    private val transactionId: String,
    private val accountRepository: AccountRepository,
    private val transactionUpdater: UpdateTransactionUseCase,
    private val transactionFinder: FindTransactionUseCase,
    private val transactionDeleter: DeleteTransactionUseCase,
    private val accountFinder: FindAccountUseCase,
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
        copy(isEnabled = amount.formatInputToDouble() >= 1 && date.isNotEmpty() && description.isNotEmpty())

    private fun loadCurrentTransaction() = viewModelScope.launch {
        val accounts: List<Account> = accountRepository.all().firstOrNull() ?: emptyList()
        oldTransaction = transactionFinder(transactionId) ?: return@launch
        oldAccount = accountFinder(oldTransaction.accountId) ?: return@launch
        dateInLong = oldTransaction.date
        updateState {
            copy(
                amount = TextFieldValue(oldTransaction.amountDecimalFormat),
                description = oldTransaction.description,
                date = millisToReadableFormat(oldTransaction.date),
                transactionType = oldTransaction.type,
                accounts = accounts,
                accountSelected = oldAccount,
            ).recomputeValidity()
        }
    }

    private fun updateTransaction() = viewModelScope.launch {
        try {
            transactionUpdater(oldTransaction, createTransactionUpdate())
            sendEffect(EditTransactionEffect.TransactionUpdated)
        } catch (e: DomainException) {
            sendEffect(EditTransactionEffect.ShowError(e.toUserMessage()))
        } catch (e: Exception) {
            sendEffect(EditTransactionEffect.ShowError(DomainException.Unknown(e).toUserMessage()))
        }
    }

    private fun createTransactionUpdate(): TransactionUpdate = TransactionUpdate(
        type = currentState.transactionType,
        description = currentState.description,
        date = dateInLong,
        amount = currentState.amount.formatInputToDouble(),
        accountId = currentState.accountSelected?.accountId ?: throw IllegalStateException(),
        categoryId = null,
    )

    private fun deleteTransaction() = viewModelScope.launch {
        try {
            transactionDeleter(oldTransaction.transactionId)
            sendEffect(EditTransactionEffect.TransactionDeleted)
        } catch (e: DomainException) {
            sendEffect(EditTransactionEffect.ShowError(e.toUserMessage()))
        } catch (e: Exception) {
            sendEffect(EditTransactionEffect.ShowError(DomainException.Unknown(e).toUserMessage()))
        }
    }

    private fun updateCurrentDate(millis: Long?) = millis?.let {
        dateInLong = it
        updateState { copy(date = DateUtils.millisToReadableFormatUTC(it)) }
    }
}
