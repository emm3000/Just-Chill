package com.emm.justchill.hh.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.auth.domain.AuthRepository
import com.emm.justchill.hh.account.presentation.Account
import com.emm.justchill.hh.auth.presentation.Login
import com.emm.justchill.hh.category.presentation.Category
import com.emm.justchill.hh.home.Home
import com.emm.justchill.hh.me.MeNavigation
import com.emm.justchill.hh.shared.seetransactions.SeeTransactionsVersionTwo
import com.emm.justchill.hh.transaction.presentation.EditTransaction
import com.emm.justchill.hh.transaction.presentation.Transaction
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object PreLogin

@Serializable
object Login

@Serializable
object Main

@Serializable
data class EditTransaction(val transactionId: String)

@Serializable
object Category

@Serializable
object Account

@Composable
fun Hh() {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Main) {
        composable<PreLogin> {
            val repository: AuthRepository = koinInject()

            LaunchedEffect(Unit) {
                repository.session()?.let {
                    navController.navigate(Main) {
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
                Text(text = "Verificando la sesión")
            }
        }
        composable<Login> {
            Login(navController)
        }
        composable<Main> {
            val internalNavController = rememberNavController()
            Scaffold(
                bottomBar = { Csm(internalNavController) }
            ) { paddingValues ->
                NavHost(
                    navController = internalNavController,
                    startDestination = HhRoutes.HhHome.route,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable(HhRoutes.HhHome.route) {
                        Home(
                            navigateToCreateAccount = {
                                navController.navigate(Account)
                            },
                            navigateToCreateCategory = {
                                navController.navigate(Category)
                            }
                        )
                    }
                    composable(HhRoutes.AddTransaction.route) {
                        Transaction {
                            internalNavController.navigate(HhRoutes.SeeTransaction.route) {
                                popUpTo(internalNavController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    }
                    composable(HhRoutes.SeeTransaction.route) {
                        SeeTransactionsVersionTwo(
                            navController
                        )
                    }
                }
            }
        }
        composable<EditTransaction> {
            val editTransaction: EditTransaction = it.toRoute<EditTransaction>()
            EditTransaction(navController, editTransaction.transactionId)
        }
        composable<Category> {
            Category(navController)
        }
        composable<Account> {
            Account(navController)
        }
    }
}

@Composable
private fun Csm(internalNavController: NavHostController) {
    val navBackStackEntry by internalNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        hhRoutes.forEach { screen ->
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    internalNavController.navigate(screen.route) {
                        popUpTo(internalNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        screen.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = LocalContentColor.current,
                    )
                },
                label = {
                    Text(
                        text = screen.name,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = LocalContentColor.current,
                        fontFamily = LatoFontFamily,
                        fontWeight = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            )
        }
    }
}