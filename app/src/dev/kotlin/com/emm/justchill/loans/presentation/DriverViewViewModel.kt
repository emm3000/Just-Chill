package com.emm.justchill.loans.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.daily.DailyUi
import com.emm.justchill.daily.domain.Daily
import com.emm.justchill.daily.domain.DailyRepository
import com.emm.justchill.daily.domain.Driver
import com.emm.justchill.daily.domain.DriverRepository
import com.emm.justchill.daily.toUi
import com.emm.justchill.hh.domain.shared.DateAndTimeCombiner
import com.emm.justchill.loans.domain.Loan
import com.emm.justchill.loans.domain.LoanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*

class DriverViewViewModel(
    driverRepository: DriverRepository,
    private val loanRepository: LoanRepository,
    private val dailyRepository: DailyRepository,
    private val driverId: Long,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) : ViewModel() {

    val currentDriver: StateFlow<Driver?> = driverRepository.find(driverId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    val driversLoansAndDailies: StateFlow<Pair<List<LoanUi>, List<DailyUi>>> = combine(
        loanRepository.retrieveByDriverId(driverId),
        dailyRepository.retrieveBy(driverId),
    ) { loans, dailies ->
        Pair(
            first = loans
                .filter { it.driverId == driverId && it.status == "PENDING" }
                .map(Loan::toUi),
            second = dailies
                .filter { it.driverId == driverId }
                .take(5)
                .map(Daily::toUi)
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Pair(emptyList(), emptyList()),
        )

    fun addDaily(driverId: Long, amount: String) = viewModelScope.launch {
        val daily = Daily(
            dailyId = UUID.randomUUID().toString(),
            amount = amount.toDouble(),
            dailyDate = dateAndTimeCombiner.combineDefaultZone(Instant.now().toEpochMilli()),
            driverId = driverId
        )
        dailyRepository.insert(daily)
    }

    fun deleteLoan(loanId: String) = viewModelScope.launch {
        loanRepository.delete(loanId)
    }

    fun deleteDaily(dailyId: String) = viewModelScope.launch {
        dailyRepository.deleteBy(dailyId)
    }
}