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
import com.emm.justchill.quota.AddQuoteScreen
import com.emm.justchill.quota.DriversScreen
import com.emm.justchill.quota.QuotasScreen
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
data class Payments(val loanId: String)

@Serializable
object Drivers

@Serializable
data class AddQuota(val driverId: Long)

@Serializable
data class Quotas(val driverId: Long)

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
                navigateToQuota = {
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
            PaymentsScreen(payments = paymentUis)
        }
        composable<Drivers> {
            DriversScreen(
                navigateToSeeQuotas = {
                    navController.navigate(Quotas(it))
                },
                navigateToAddQuotas = {
                    navController.navigate(AddQuota(it))
                },
                navigateToAddLoans = {
                    navController.navigate(AddLoan(it))
                },
                navigateToSeeLoans = {
                    navController.navigate(Loans(it))
                }
            )
        }
        composable<Loans> {
            val loans: Loans = it.toRoute<Loans>()
            LoansScreen(
                driverId = loans.driverId,
                navigateToPayments = {
                    navController.navigate(Payments(it))
                }
            )
        }
        composable<AddQuota> {
            val addQuota: AddQuota = it.toRoute<AddQuota>()
            AddQuoteScreen(
                driverId = addQuota.driverId,
                navigateToBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<Quotas> {
            val quotas: Quotas = it.toRoute<Quotas>()
            QuotasScreen(
                driverId = quotas.driverId,
            )
        }
    }
}