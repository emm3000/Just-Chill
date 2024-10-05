package com.emm.justchill.me

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.me.loan.presentation.AddLoanScreen
import com.emm.justchill.hh.me.loan.presentation.LoansScreen
import com.emm.justchill.hh.me.home.HomeScreen
import com.emm.justchill.hh.me.payment.presentation.PaymentsScreen
import com.emm.justchill.hh.me.payment.presentation.PaymentsViewModel
import com.emm.justchill.hh.me.daily.presentation.AddDailyScreen
import com.emm.justchill.hh.me.driver.presentation.DriversScreen
import com.emm.justchill.hh.me.daily.presentation.DailyScreen
import com.emm.justchill.hh.me.daily.presentation.DailyUi
import com.emm.justchill.hh.me.driver.domain.Driver
import com.emm.justchill.hh.me.driver.presentation.DriverViewScreen
import com.emm.justchill.hh.me.driver.presentation.DriverViewViewModel
import com.emm.justchill.hh.me.home.HomeViewModel
import com.emm.justchill.hh.me.loan.presentation.LoanUi
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
object LoansHome

@Serializable
data class AddLoan(val driverId: Long)

@Serializable
data class Loans(val driverId: Long)

@Serializable
data class Payments(val loanId: String, val driverName: String)

@Serializable
object Drivers

@Serializable
data class DriverView(val driverId: Long)

@Serializable
data class AddDaily(val driverId: Long)

@Serializable
data class Daily(val driverId: Long)

class MeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmmTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    LoansNavigation()
                }
            }
        }
    }
}

@Composable
fun LoansNavigation() {

    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = LoansHome) {
        composable<LoansHome> {
            val vm: HomeViewModel = koinViewModel()

            val context: Context = LocalContext.current

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri ?: return@rememberLauncherForActivityResult
                val inputStream = context.contentResolver.openInputStream(uri)
                val json: String = inputStream?.bufferedReader().use { it?.readText() }.orEmpty()
                vm.import(json)
            }

            val drivers: List<Driver> by vm.drivers.collectAsStateWithLifecycle()
            HomeScreen(
                drivers = drivers,
                navigateToDriverView = { navController.navigate(DriverView(it)) },
                saveData = vm::export,
                selectFile = {
                    launcher.launch(arrayOf("application/json"))
                }
            )
        }
        composable<AddLoan> {
            val addLoan: AddLoan = it.toRoute<AddLoan>()
            AddLoanScreen(
                driverId = addLoan.driverId,
                navigateToBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<DriverView> {
            val driverView: DriverView = it.toRoute<DriverView>()
            val vm: DriverViewViewModel = koinViewModel(
                parameters = { parametersOf(driverView.driverId) }
            )
            val driver: Driver? by vm.currentDriver.collectAsStateWithLifecycle()
            val driversLoansAndDailies: Pair<List<LoanUi>, List<DailyUi>> by vm.driversLoansAndDailies.collectAsStateWithLifecycle()
            driver?.let { notNullDriver ->
                DriverViewScreen(
                    driver = notNullDriver,
                    driversLoansAndDailies = driversLoansAndDailies,
                    addDaily = vm::addDaily,
                    deleteLoan = vm::deleteLoan,
                    deleteDaily = vm::deleteDaily,
                    navigateToSeeDailies = { driverId ->
                        navController.navigate(Daily(driverId))
                    },
                    navigateToAddLoans = { driverId ->
                        navController.navigate(AddLoan(driverId))
                    },
                    navigateToSeePayments = { loanId, driverName ->
                        navController.navigate(Payments(loanId, driverName))
                    },
                    navigateToAddDailies = { driverId ->
                        navController.navigate(AddDaily(driverId))
                    }
                )
            }
        }
        composable<Payments> {
            val payments: Payments = it.toRoute<Payments>()
            val vm: PaymentsViewModel = koinViewModel(
                parameters = { parametersOf(payments.loanId) }
            )
            val paymentUis by vm.payments.collectAsState()
            PaymentsScreen(
                payments = paymentUis,
                markPay = vm::pay,
                driverName = payments.driverName
            )
        }
        composable<Drivers> {
            DriversScreen(
                navigateToSeeDailies = {
                    navController.navigate(Daily(it))
                },
                navigateToAddLoans = {
                    navController.navigate(AddLoan(it))
                },
                navigateToSeePayments = { loanId, driverName ->
                    navController.navigate(Payments(loanId, driverName))
                }
            )
        }
        composable<Loans> {
            val loans: Loans = it.toRoute<Loans>()
            LoansScreen(
                driverId = loans.driverId,
                navigateToPayments = { loanId ->
                    navController.navigate(Payments(loanId, ""))
                }
            )
        }
        composable<AddDaily> {
            val addDaily: AddDaily = it.toRoute<AddDaily>()
            AddDailyScreen(
                driverId = addDaily.driverId,
                navigateToBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<Daily> {
            val daily: Daily = it.toRoute<Daily>()
            DailyScreen(
                driverId = daily.driverId,
            )
        }
    }
}