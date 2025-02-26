package com.emm.justchill.hh.me.driver.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.account.domain.DailyAccountCreator
import com.emm.justchill.hh.shared.DateAndTimeCombiner
import com.emm.justchill.hh.transaction.domain.TransactionCreator
import com.emm.justchill.hh.transaction.domain.TransactionInsert
import com.emm.justchill.hh.transaction.presentation.TransactionType
import com.emm.justchill.hh.me.loan.domain.Loan
import com.emm.justchill.hh.me.loan.domain.LoanRepository
import com.emm.justchill.hh.me.loan.presentation.LoanUi
import com.emm.justchill.hh.me.driver.domain.Driver
import com.emm.justchill.hh.me.driver.domain.DriverRepository
import com.emm.justchill.hh.me.daily.domain.Daily
import com.emm.justchill.hh.me.daily.domain.DailyRepository
import com.emm.justchill.hh.me.export.DataExporter
import com.emm.justchill.hh.me.daily.presentation.DailyUi
import com.emm.justchill.hh.me.daily.presentation.toUi
import com.emm.justchill.hh.me.loan.presentation.toUi
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

data class DriversScreenUi(
    val loans: List<LoanUi>,
    val dailies: List<DailyUi>,
)

class DriversViewModel(
    private val driverRepository: DriverRepository,
    private val dailyRepository: DailyRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val loanRepository: LoanRepository,
    private val dataExporter: DataExporter,
    private val transactionCreator: TransactionCreator,
    private val dailyAccountCreator: DailyAccountCreator,
) : ViewModel() {

    val drivers: StateFlow<Map<Driver, DriversScreenUi>> = combine(
        driverRepository.all(),
        loanRepository.all(),
        dailyRepository.all(),
    ) { drivers, loans, dailies ->
        drivers.associateWith { driver ->
            DriversScreenUi(
                loans = loans
                    .filter { it.driverId == driver.driverId && it.status == "PENDING" }
                    .map(Loan::toUi),
                dailies = dailies
                    .filter { it.driverId == driver.driverId }
                    .take(2)
                    .map(Daily::toUi)
            )
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
        val dailyDate = dateAndTimeCombiner.combineDefaultZone(Instant.now().toEpochMilli())
        val daily = Daily(
            dailyId = UUID.randomUUID().toString(),
            amount = amount.toDouble(),
            dailyDate = dailyDate,
            driverId = driverId
        )
        dailyRepository.insert(daily)
        resolveTransactionAndAccounts(amount, dailyDate, driverId)
    }

    fun deleteLoan(loanId: String) = viewModelScope.launch {
        loanRepository.delete(loanId)
    }

    fun export() = viewModelScope.launch {
        dataExporter.export()
    }

    fun import(json: String) = viewModelScope.launch {
        dataExporter.import(json)
    }

    fun deleteDaily(dailyId: String) = viewModelScope.launch {
        dailyRepository.deleteBy(dailyId)
    }

    private suspend fun resolveTransactionAndAccounts(
        amount: String,
        dailyDate: Long,
        driverId: Long
    ) {
        val driver: Driver = drivers.value.keys.firstOrNull { it.driverId == driverId } ?: return

        val accountId = dailyAccountCreator.create()

        val transactionInsert = TransactionInsert(
            type = TransactionType.INCOME,
            amount = amount.toDouble(),
            description = "Feria de ${driver.name}",
            date = dailyDate,
            accountId = accountId
        )
        transactionCreator.create(transactionInsert)
    }
}