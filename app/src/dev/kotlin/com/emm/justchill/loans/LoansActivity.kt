package com.emm.justchill.loans

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.loans.presentation.AddLoanScreen
import com.emm.justchill.loans.presentation.LoansScreen
import com.emm.justchill.loans.presentation.ShortcutHomeScreen
import com.emm.justchill.loans.presentation.PaymentsScreen
import com.emm.justchill.loans.presentation.PaymentsViewModel
import com.emm.justchill.daily.AddDailyScreen
import com.emm.justchill.daily.DriversScreen
import com.emm.justchill.daily.DailyScreen
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
data class AddDaily(val driverId: Long)

@Serializable
data class Daily(val driverId: Long)

class LoansActivity : AppCompatActivity() {

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

    NavHost(navController = navController, startDestination = Drivers) {
        composable<LoansHome> {
            ShortcutHomeScreen(
                navigateToLoans = {
                    navController.navigate(AddLoan)
                },
                navigateToPayments = {
                    navController.navigate(Payments)
                },
                navigateToDaily = {
                    navController.navigate(Drivers)
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
        composable<Payments> {
            val payments: Payments = it.toRoute<Payments>()
            val vm: PaymentsViewModel = koinViewModel(
                parameters = { parametersOf(payments.loanId) }
            )
            val paymentUis by vm.payments.collectAsState()
            PaymentsScreen(payments = paymentUis, markPay = vm::pay, driverName = payments.driverName)
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
                navigateToPayments = {
                    navController.navigate(Payments(it, ""))
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