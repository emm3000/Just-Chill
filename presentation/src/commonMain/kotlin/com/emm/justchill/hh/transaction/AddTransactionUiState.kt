package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.shared.relativeDayLabel
import kotlinx.datetime.LocalDate

data class AddTransactionUiState(
    val today: LocalDate,
    /**
     * The day the transaction is recorded for, or `null` while unpicked. `null` is not "no date" —
     * it is *the day this gets saved on*, staying unresolved until the save happens.
     */
    val date: LocalDate? = null,
    val amount: String = "",
    val description: String = String.Empty,
    val transactionType: TransactionType = TransactionType.Spend,
    val isEnabled: Boolean = false,
    val hasChanges: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val accountSelected: Account? = null,
    val categories: List<SelectableCategory> = emptyList(),
    val categorySelected: SelectableCategory? = null,
    val frequentCategoryIds: List<String> = emptyList(),
    val frequentCombos: List<FrequentComboUi> = emptyList(),
) : UiState {
    val dateLabel: String get() = date?.let { relativeDayLabel(it, today) } ?: "Hoy"

    val pickerDate: LocalDate get() = date ?: today

    val missingField: MissingField? get() = when {
        centsToSoles(amount) <= 0.0 -> MissingField.Amount
        accountSelected == null -> MissingField.Account
        else -> null
    }
}

enum class MissingField { Amount, Account }
