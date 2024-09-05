package com.emm.justchill.hh.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.data.transaction.TransactionInsert
import com.emm.justchill.hh.domain.transaction.TransactionType
import com.emm.justchill.hh.domain.transaction.TransactionAdder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionAdder: TransactionAdder,
) : ViewModel() {

    var amount by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var isEnabled by mutableStateOf(false)
        private set

    var transactionType by mutableStateOf(TransactionType.INCOME)
        private set

    init {
        combine(
            snapshotFlow { amount },
            snapshotFlow { date },
            snapshotFlow { description },
        ) { mount, date, description ->
            isEnabled = mount.isNotEmpty()
                    && date.isNotEmpty()
                    && description.isNotEmpty()
        }.launchIn(viewModelScope)
    }

    fun addTransaction() = viewModelScope.launch {
        val transactionInsert = TransactionInsert(
            type = transactionType.name,
            description = description,
            date = dateInLong,
        )
        transactionAdder.add(amount, transactionInsert)
    }

    fun updateMount(value: String) {
        amount = value
    }

    fun updateDescription(value: String) {
        description = value
    }

    fun updateCurrentDate(millis: Long?) {
        if (millis != null) {
            dateInLong = millis
            date = DateUtils.millisToReadableFormatUTC(millis)
        }
    }

    fun updateTransactionType(value: TransactionType) {
        transactionType = value
    }
}