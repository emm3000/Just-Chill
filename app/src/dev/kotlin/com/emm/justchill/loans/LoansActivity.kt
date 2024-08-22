package com.emm.justchill.loans

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.loans.presentation.Loans
import com.emm.justchill.loans.presentation.LoansHome
import com.emm.justchill.loans.presentation.Payments
import kotlinx.serialization.Serializable

@Serializable
object LoansHome

@Serializable
object Loans

@Serializable
object Payments

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

    NavHost(navController = navController, startDestination = LoansHome) {
        composable<LoansHome> {
            LoansHome(
                navigateToLoans = {
                    navController.navigate(Loans)
                },
                navigateToPayments = {
                    navController.navigate(Payments)
                }
            )
        }
        composable<Loans> {
            Loans()
        }
        composable<Payments> {
            Payments(payments = emptyList())
        }
    }
}