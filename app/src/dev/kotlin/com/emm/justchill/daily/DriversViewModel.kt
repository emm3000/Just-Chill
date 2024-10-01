package com.emm.justchill.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.domain.shared.DateAndTimeCombiner
import com.emm.justchill.loans.domain.Loan
import com.emm.justchill.loans.domain.LoanRepository
import com.emm.justchill.loans.presentation.LoanUi
import com.emm.justchill.loans.presentation.toUi
import com.emm.justchill.daily.domain.Driver
import com.emm.justchill.daily.domain.DriverRepository
import com.emm.justchill.daily.domain.Daily
import com.emm.justchill.daily.domain.DailyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*

val staticDrivers = listOf(
    Driver(
        driverId = 1,
        name = "Sergio",
    ),
    Driver(
        driverId = 2,
        name = "Soldado",
    ),
    Driver(
        driverId = 3,
        name = "Nehemias",
    ),
    Driver(
        driverId = 4,
        name = "Samora",
    )
)

class DriversViewModel(
    private val driverRepository: DriverRepository,
    private val dailyRepository: DailyRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val loanRepository: LoanRepository,
) : ViewModel() {

    val drivers: StateFlow<Map<Driver, List<LoanUi>>> = combine(
        driverRepository.all(),
        loanRepository.all()
    ) { drivers, loans ->
        drivers.associateWith { driver ->
            loans
                .filter { it.driverId == driver.driverId && it.status == "PENDING" }
                .map(Loan::toUi)
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = mapOf(),
        )

    init {
        insertIfNoExistData()
    }

    private fun insertIfNoExistData() = viewModelScope.launch {
        val count: List<Driver> = driverRepository.all().firstOrNull().orEmpty()
        if (count.isNotEmpty()) return@launch
        staticDrivers.forEach {
            driverRepository.insert(it)
        }
    }

    fun addDaily(driverId: Long, amount: String) = viewModelScope.launch {
        val daily = Daily(
            dailyId = UUID.randomUUID().toString(),
            amount = amount.toDouble(),
            dailyDate = dateAndTimeCombiner.combine(Instant.now().toEpochMilli()),
            driverId = driverId
        )
        dailyRepository.insert(daily)
    }

    fun deleteLoan(loanId: String) = viewModelScope.launch {
        loanRepository.delete(loanId)
    }
}