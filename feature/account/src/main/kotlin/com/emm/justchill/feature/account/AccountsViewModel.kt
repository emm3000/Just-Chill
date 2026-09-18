package com.emm.justchill.feature.account

import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.domain.account.AccountRepository
import com.emm.justchill.core.domain.account.AccountUpsert
import com.emm.justchill.core.domain.account.DeleteAccountUseCase
import com.emm.justchill.core.domain.account.UpdateAccountUseCase
import com.emm.justchill.core.domain.loan.LoanRepository
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.time.TodayFlow
import com.emm.justchill.core.domain.transaction.Transaction
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.ui.error.toUserMessage
import com.emm.justchill.core.ui.loan.owingNames
import com.emm.justchill.core.ui.loan.totalOwedFormatted
import com.emm.justchill.core.ui.loan.totalOwedIsPositive
import com.emm.justchill.core.ui.mvi.MviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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

    // flatMapLatest, not combine: a query captured once at init would survive the midnight rollover
    // `month` already tracks, leaving every row attributed to the wrong month. This re-subscribes
    // instead, so a month change re-queries the bounded slice rather than only relabeling it.
    private val monthTransactions: Flow<List<Transaction>> = month
        .flatMapLatest { current ->
            transactionRepository.allInRange(current.startInclusiveDay(), current.endExclusiveDay())
        }

    init {
        combine(
            accountRepository.all(),
            monthTransactions,
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
                    copy(
                        loansTotalOwed = balances.totalOwedFormatted(),
                        loansTotalOwedIsPositive = balances.totalOwedIsPositive(),
                        loansPeople = balances.owingNames(),
                    )
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
