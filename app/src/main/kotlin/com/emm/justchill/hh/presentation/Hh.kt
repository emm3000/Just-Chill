package com.emm.justchill.hh.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.presentation.category.Category
import com.emm.justchill.hh.presentation.home.Home
import com.emm.justchill.hh.presentation.auth.Login
import com.emm.justchill.hh.presentation.seetransactions.SeeTransactions
import com.emm.justchill.hh.presentation.transaction.Transaction
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object PreLogin

@Serializable
object Login

@Serializable
object Home

@Serializable
object Income

@Serializable
object Category

@Serializable
object SeeTransactions

@Composable
fun Hh() {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = PreLogin) {
        composable<PreLogin> {
            val repository: AuthRepository = koinInject()

            LaunchedEffect(Unit) {
                delay(1000L)
                repository.session()?.let {
                    navController.navigate(Home) {
                        popUpTo<PreLogin> {
                            inclusive = true
                        }
                    }
                } ?: run {
                    navController.navigate(Login) {
                        popUpTo<PreLogin> {
                            inclusive = true
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Verificando la sessión")
            }
        }
        composable<Login> {
            Login(navController)
        }
        composable<Home> {
            Home(navController)
        }
        composable<Income> {
            Transaction(navController)
        }
        composable<Category> {
            Category(navController)
        }
        composable<SeeTransactions> {
            SeeTransactions(navController)
        }
    }
}