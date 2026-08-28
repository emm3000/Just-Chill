package com.emm.justchill.hh.account

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpsert
import com.emm.domain.account.DeleteAccountUseCase
import com.emm.domain.account.UpdateAccountUseCase
import com.emm.domain.loan.LoanRepository
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionRepository
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.time.TodayFlow
import com.emm.justchill.hh.loan.owingNames
import com.emm.justchill.hh.loan.totalOwedFormatted
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class AccountsViewModel(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    loanRepository: LoanRepository,
    private val updateAccount: UpdateAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    todayFlow: TodayFlow,
) : MviViewModel<AccountsUiState, AccountsIntent, AccountsEffect>(
    AccountsUiState(month = YearMonth.of(todayFlow.today())),
) {

    // The screen's only derivation of "what month is it": the eyebrow, the two header totals and
    // every row's net are attributed against this one value, so they cannot disagree at midnight.
    private val month: StateFlow<YearMonth> = todayFlow()
        .map { YearMonth.of(it) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialState.month)

    init {
        combine(
            accountRepository.all(),
            transactionRepository.all(),
            month,
            ::accountsMonthSlice,
        )
            .onEach { slice ->
                updateState {
                    copy(
                        month = slice.month,
                        accounts = slice.accounts,
                        monthSpent = slice.spent,
                        monthIncome = slice.income,
                    )
                }
            }
            .launchSafeIn(onError = { e -> AccountsEffect.ShowMessage(e.toUserMessage()) })

        // A separate flow on purpose (ADR 010): loans never fold into the accounts query.
        loanRepository.balancesByPerson()
            .onEach { balances ->
                updateState {
                    copy(loansTotalOwed = balances.totalOwedFormatted(), loansPeople = balances.owingNames())
                }
            }
            .launchSafeIn(onError = { e -> AccountsEffect.ShowMessage(e.toUserMessage()) })
    }

    override fun onIntent(intent: AccountsIntent) {
        when (intent) {
            is AccountsIntent.OnEditClick -> updateState {
                copy(pendingEdit = intent.account, editName = intent.account.name)
            }

            is AccountsIntent.OnEditNameChange -> updateState { copy(editName = intent.value) }

            AccountsIntent.OnEditDismiss -> updateState { copy(pendingEdit = null, editName = "") }

            AccountsIntent.OnEditConfirm -> confirmEdit()

            is AccountsIntent.OnDeleteClick -> updateState { copy(pendingDelete = intent.account) }

            AccountsIntent.OnDeleteDismiss -> updateState { copy(pendingDelete = null) }

            AccountsIntent.OnDeleteConfirm -> confirmDelete()
        }
    }

    private fun confirmEdit() = launchSafe(
        onError = { e -> AccountsEffect.ShowMessage(e.toUserMessage()) },
    ) {
        val target = currentState.pendingEdit ?: return@launchSafe
        val newName = currentState.editName
        updateAccount(
            accountId = target.accountId,
            account = AccountUpsert(
                accountId = target.accountId,
                name = newName,
                type = target.type,
            ),
        )
        updateState { copy(pendingEdit = null, editName = "") }
    }

    private fun confirmDelete() = launchSafe(
        onError = { e ->
            updateState { copy(pendingDelete = null) }
            AccountsEffect.ShowMessage(e.toUserMessage())
        },
    ) {
        val target = currentState.pendingDelete ?: return@launchSafe
        deleteAccount(target.accountId)
        updateState { copy(pendingDelete = null) }
    }
}
