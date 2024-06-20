package com.emm.justchill.experiences.hh.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.experiences.hh.data.TransactionInsert
import com.emm.justchill.experiences.hh.domain.TransactionAdder
import kotlinx.coroutines.launch
import java.time.Instant

class IncomeViewModel(private val transactionAdder: TransactionAdder) : ViewModel() {

    var mount by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf("")
        private set

    var category by mutableStateOf("")
        private set

    fun addTransaction() = viewModelScope.launch {
        val transactionInsert = TransactionInsert(
            type = category,
            mount = mount,
            description = description,
            date = Instant.now().toEpochMilli()
        )
        transactionAdder.add(transactionInsert)
    }

    fun updateA(value: String) {
        mount = value
    }

    fun updateB(value: String) {
        description = value
    }

    fun updateC(value: String) {
        date = value
    }

    fun updateD(value: String) {
        category = value
    }
}