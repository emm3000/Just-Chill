package com.emm.justchill.hh.presentation.seetransactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.Transactions
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.presentation.transaction.TransactionUi
import com.emm.justchill.hh.presentation.transaction.toUi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SeeTransactionsVersionTwoViewModel(repository: TransactionRepository) : ViewModel() {

    val transactions: StateFlow<List<TransactionUi>> = repository.all()
        .map(List<Transactions>::toUi)
        .catch(::catchThrowable)
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