package com.emm.justchill.hh.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.domain.BackupManager
import com.emm.justchill.hh.domain.transaction.TransactionDifferenceCalculator
import com.emm.justchill.hh.domain.transaction.TransactionSumIncome
import com.emm.justchill.hh.domain.transaction.TransactionSumSpend
import com.emm.justchill.hh.domain.transaction.fromCentsToSolesWith
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    transactionSumIncome: TransactionSumIncome,
    transactionSumSpend: TransactionSumSpend,
    transactionDifferenceCalculator: TransactionDifferenceCalculator,
    private val backupManager: BackupManager,
) : ViewModel() {

    val sumTransactions: StateFlow<Pair<String, String>> = combine(
        flow = transactionSumIncome(),
        flow2 = transactionSumSpend(),
        transform = ::Pair
    )
        .map {
            val first = fromCentsToSolesWith(it.first)
            val second = fromCentsToSolesWith(it.second)
            Pair(first, second)
        }
        .catch {
            it.printStackTrace()
            emit(Pair("0.00", "0.00"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Pair("0.00", "0.00")
        )

    val difference = transactionDifferenceCalculator.calculate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = "0.00"
        )

    init {
        viewModelScope.launch {
            backupManager.seed()
        }
    }
}