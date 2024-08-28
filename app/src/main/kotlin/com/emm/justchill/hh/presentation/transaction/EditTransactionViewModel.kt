package com.emm.justchill.hh.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.data.transaction.TransactionUpdate
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.domain.transaction.TransactionDeleter
import com.emm.justchill.hh.domain.transaction.TransactionFinder
import com.emm.justchill.hh.domain.transaction.TransactionUpdater
import com.emm.justchill.hh.domain.transaction.fromCentsToSoles
import com.emm.justchill.hh.domain.transactioncategory.AmountDbFormatter
import com.emm.justchill.hh.presentation.transaction.DateUtils.millisToReadableFormat
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class EditTransactionViewModel(
    private val transactionId: String,
    private val transactionUpdater: TransactionUpdater,
    private val transactionFinder: TransactionFinder,
    private val amountCleaner: AmountDbFormatter,
    private val transactionDeleter: TransactionDeleter,
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

        loadCurrentTransaction()
    }

    private fun loadCurrentTransaction() = viewModelScope.launch {
        val currentTransaction = transactionFinder.find(transactionId).firstOrNull()
        currentTransaction?.let { transaction ->
            amount = transaction.amount.toString()
            description = transaction.description
            transactionType = TransactionType.valueOf(transaction.type)
            date = millisToReadableFormat(transaction.date)
            dateInLong = transaction.date
        }
    }

    fun updateTransaction() = viewModelScope.launch {
        val transactionUpdate = TransactionUpdate(
            type = transactionType.name,
            description = description,
            date = dateInLong,
            amount = amount.replace(Regex("[^\\d.]"), "").toDouble()
        )
        transactionUpdater.update(transactionId, transactionUpdate)
    }

    fun deleteTransaction() = viewModelScope.launch {
        transactionDeleter.delete(transactionId)
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