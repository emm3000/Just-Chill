package com.emm.justchill.hh.seetransactions

import androidx.lifecycle.viewModelScope
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.TransactionUi
import com.emm.justchill.hh.transaction.toUi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DayGroup(
    val date: LocalDate,
    val transactions: List<TransactionUi>,
) {

    val readableDate: String
        get() {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            return when (date) {
                today -> "HOY"
                yesterday -> "AYER"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("MMMM dd", Locale.forLanguageTag("es"))
                    date.format(formatter).uppercase()
                }
            }
        }
}

class SeeTransactionsViewModel(
    transactionRepository: TransactionRepository,
) : MviViewModel<SeeTransactionsUiState, SeeTransactionsIntent, SeeTransactionsEffect>(SeeTransactionsUiState()) {

    init {
        transactionRepository.fetchAllWithCategory()
            .map(::groupByDate)
            .map(::mapToDayGroup)
            .catch { emit(emptyList()) }
            .onEach { days -> updateState { copy(days = days) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SeeTransactionsIntent) = Unit
}

private fun mapToDayGroup(
    transactionGroups: Map<LocalDate, List<TransactionWithCategory>>,
): List<DayGroup> = transactionGroups.map { (date, transactions) ->
    DayGroup(date = date, transactions = transactions.toUi())
}

private fun groupByDate(
    categories: List<TransactionWithCategory>,
): Map<LocalDate, List<TransactionWithCategory>> = categories.groupBy { transaction ->
    Instant.ofEpochMilli(transaction.date)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
