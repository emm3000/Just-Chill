package com.emm.justchill.experiences.hh.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.Transactions
import com.emm.justchill.core.Result
import com.emm.justchill.experiences.hh.domain.TransactionLoader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(transactionLoader: TransactionLoader) : ViewModel() {

    val transactions: StateFlow<Result<List<Transactions>>> = transactionLoader.load()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Result.Loading
        )
}