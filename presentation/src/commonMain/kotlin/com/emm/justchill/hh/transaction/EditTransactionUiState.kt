package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.shared.relativeDayLabel
import kotlinx.datetime.LocalDate

data class EditTransactionUiState(
    /**
     * The day the transaction is recorded for — the stored one until the user picks another.
     * This is the date; the label below is derived from it, and the save path and the date picker
     * both read it.
     *
     * It has no default: the state is not the place that asks a clock what day it is. The
     * ViewModel supplies today from its injected [kotlin.time.Clock] until the load replaces it
     * with the transaction's own day.
     */
    val date: LocalDate,
    /** Reference day for [dateLabel]'s Hoy/Ayer branch, from the same clock as [date]. */
    val today: LocalDate,
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
) : UiState {
    /** "Hoy" / "Ayer" / "13 jun" — derived, so it cannot drift away from [date]. */
    val dateLabel: String get() = relativeDayLabel(date, today)

    val missingField: MissingField? get() = when {
        centsToSoles(amount) <= 0.0 -> MissingField.Amount
        accountSelected == null -> MissingField.Account
        else -> null
    }
}
