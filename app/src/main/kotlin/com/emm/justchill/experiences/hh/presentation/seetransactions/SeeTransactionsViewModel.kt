package com.emm.justchill.experiences.hh.presentation.seetransactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.Transactions
import com.emm.justchill.core.Result
import com.emm.justchill.core.mapResult
import com.emm.justchill.experiences.hh.domain.transaction.EndDateMillis
import com.emm.justchill.experiences.hh.domain.transaction.StartDateMillis
import com.emm.justchill.experiences.hh.domain.transaction.TransactionLoaderByDateRange
import com.emm.justchill.experiences.hh.presentation.transaction.DateUtils
import com.emm.justchill.experiences.hh.presentation.transaction.TransactionUi
import com.emm.justchill.experiences.hh.presentation.transaction.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DateDataHolder(
    val millisDate: Long = 0L,
    val readableDate: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
class SeeTransactionsViewModel(
    transactionLoaderByDateRange: TransactionLoaderByDateRange,
) : ViewModel() {

    var holderForStartDate: DateDataHolder by mutableStateOf(DateDataHolder())
        private set

    var holderForEndDate: DateDataHolder by mutableStateOf(DateDataHolder())
        private set

    val transactions: StateFlow<Result<List<TransactionUi>>> = combine(
        snapshotFlow { holderForStartDate },
        snapshotFlow { holderForEndDate },
    ) { startDate, endDate ->
        val startDateMillis = StartDateMillis(startDate.millisDate)
        val endDateMillis = EndDateMillis(endDate.millisDate)
        Pair(startDateMillis, endDateMillis)
    }
        .flatMapLatest { (startDate, endData) ->
            transactionLoaderByDateRange.load(startDate, endData)
        }
        .mapResult(List<Transactions>::toUi)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Result.Loading
        )

    fun updateDataHolder(millis: Long?) {
        if (millis != null) {
            val readableFormat: String = DateUtils.millisToReadableFormat(millis)
            holderForStartDate = DateDataHolder(millis, readableFormat)
        }
    }

    fun updateDataHolder2(millis: Long?) {
        if (millis != null) {
            val readableFormat: String = DateUtils.millisToReadableFormat(millis)
            holderForEndDate = DateDataHolder(millis, readableFormat)
        }
    }
}