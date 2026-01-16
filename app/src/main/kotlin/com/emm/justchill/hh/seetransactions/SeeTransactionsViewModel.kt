package com.emm.justchill.hh.seetransactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.hh.transaction.TransactionUi
import com.emm.justchill.hh.transaction.toUi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayGroup(
    val date: LocalDate,
    val transactions: List<TransactionUi>,
)

class SeeTransactionsViewModel(
    transactionRepository: TransactionRepository,
) : ViewModel() {

    val transactions: StateFlow<List<DayGroup>> = transactionRepository.fetchAllWithCategory()
        .map {
            it.groupBy {
                Instant.ofEpochMilli(it.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        }
        .map {
            it.map { (date, transactions: List<TransactionWithCategory>) ->
                DayGroup(
                    date = date,
                    transactions = transactions.toUi()
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    @Suppress("UNUSED_PARAMETER")
    private suspend fun catchThrowable(
        collector: FlowCollector<List<TransactionUi>>,
        throwable: Throwable,
    ) = collector.emit(emptyList())
}