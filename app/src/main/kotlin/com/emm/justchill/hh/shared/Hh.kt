package com.emm.justchill.hh.shared

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.account.presentation.Account
import com.emm.justchill.hh.category.presentation.Category
import com.emm.justchill.hh.home.Home
import com.emm.justchill.hh.shared.seetransactions.SeeTransactionsVersionTwo
import com.emm.justchill.hh.shared.shared.Account
import com.emm.justchill.hh.shared.shared.Category
import com.emm.justchill.hh.shared.shared.EditTransaction
import com.emm.justchill.hh.transaction.presentation.EditTransaction
import com.emm.justchill.hh.transaction.presentation.Transaction

@Composable
fun Hh() {

    val navController = rememberNavController()

    Scaffold(bottomBar = { Csm(navController) }) { paddingValues ->
        NavHost(
            navController = navController,
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
                    navController.navigate(HhRoutes.SeeTransaction.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                }
            }
            composable(HhRoutes.SeeTransaction.route) {
                SeeTransactionsVersionTwo(navController)
            }
            composable<EditTransaction> {
                val editTransaction: EditTransaction = it.toRoute<EditTransaction>()
                EditTransaction(navController, editTransaction.transactionId)
            }
            composable<Account> {
                Account(navController)
            }
            composable<Category> {
                Category(navController)
            }
        }
    }
}

@Composable
private fun Csm(internalNavController: NavHostController) {
    val navBackStackEntry: NavBackStackEntry? by internalNavController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination

    if (currentDestination?.route !in hhRoutes.map { it.route }) return

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