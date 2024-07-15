package com.emm.justchill.hh.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.domain.AndroidDataProvider
import com.emm.justchill.hh.domain.transaction.TransactionDifferenceCalculator
import com.emm.justchill.hh.domain.transaction.TransactionSumIncome
import com.emm.justchill.hh.domain.transaction.TransactionSumSpend
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

class HomeViewModel(
    transactionSumIncome: TransactionSumIncome,
    transactionSumSpend: TransactionSumSpend,
    transactionDifferenceCalculator: TransactionDifferenceCalculator,
    androidDataProvider: AndroidDataProvider,
) : ViewModel() {

    val androidId = androidDataProvider.androidId()

    val sumTransactions: StateFlow<Pair<String, String>> = combine(
        transactionSumIncome(),
        transactionSumSpend(),
    ) { income: BigDecimal, spend: BigDecimal ->
        Pair(income.toString(), spend.toString())
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
        .map { it.toString() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = "0.00"
        )
}