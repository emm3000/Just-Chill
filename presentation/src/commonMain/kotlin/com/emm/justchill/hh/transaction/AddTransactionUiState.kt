package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.shared.relativeDayLabel
import kotlinx.datetime.LocalDate

data class AddTransactionUiState(
    /**
     * Today, as of the last time the ViewModel's clock was read. Reference day for [dateLabel]'s
     * Hoy/Ayer branch, and what an unset [date] resolves to.
     *
     * It has no default: there is no answer without asking a clock, and a state class is not what
     * asks. The ViewModel supplies it from its injected [kotlin.time.Clock].
     */
    val today: LocalDate,
    /**
     * The day the transaction is being recorded for, or `null` while the user has not picked one.
     *
     * `null` is not "no date" — it is *the day this gets saved on*, and it stays unresolved until
     * the save happens. Resolving it up front is what made a screen opened at 23:59 and saved at
     * 00:01 book the movement on the previous day, silently and with the label still reading "Hoy".
     */
    val date: LocalDate? = null,
    val amount: String = "",
    val description: String = String.Empty,
    val transactionType: TransactionType = TransactionType.Income,
    val isEnabled: Boolean = false,
    val hasChanges: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val accountSelected: Account? = null,
    val categories: List<SelectableCategory> = emptyList(),
    val categorySelected: SelectableCategory? = null,
    val frequentCategoryIds: List<String> = emptyList(),
    val frequentCombos: List<FrequentComboUi> = emptyList(),
) : UiState {
    /**
     * "Hoy" / "Ayer" / "13 jun" — derived, so it cannot drift away from [date].
     *
     * An unset [date] is "Hoy" without consulting anything, which is the one label that cannot go
     * stale: it stays true across midnight because it names the save, not a day.
     */
    val dateLabel: String get() = date?.let { relativeDayLabel(it, today) } ?: "Hoy"

    /** The day the picker should open on: the picked one, or today when nothing is picked yet. */
    val pickerDate: LocalDate get() = date ?: today

    val missingField: MissingField? get() = when {
        centsToSoles(amount) <= 0.0 -> MissingField.Amount
        accountSelected == null -> MissingField.Account
        else -> null
    }
}

enum class MissingField { Amount, Account }
