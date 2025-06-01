package com.emm.justchill.hh.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.domain.account.Account
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.account.AddAccountScreen
import com.emm.justchill.hh.category.CategoryScreen
import com.emm.justchill.hh.fasttransaction.AccountsScreen
import com.emm.justchill.hh.fasttransaction.AccountsViewModel
import com.emm.justchill.hh.home.Home
import com.emm.justchill.hh.seetransactions.SeeTransactionsVersionTwo
import com.emm.justchill.hh.shared.shared.Screen
import com.emm.justchill.hh.transaction.EditTransaction
import com.emm.justchill.hh.transaction.TransactionScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun Hh() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard,
    ) {
        composable<Screen.Login> {
            DashboardContent(navController)
        }
        composable<Screen.Dashboard> {
            DashboardContent(navController)
        }
        composable(HhRoutes.AddTransaction.route) {
            TransactionScreen(
                popBackStack = { navController.popBackStack() }
            )
        }
        composable<Screen.EditTransaction> {
            val editTransaction: Screen.EditTransaction = it.toRoute<Screen.EditTransaction>()
            EditTransaction(navController, editTransaction.transactionId)
        }
        composable(HhRoutes.AddAccount.route) {
            AddAccountScreen(navController)
        }
        composable<Screen.Category> {
            CategoryScreen(navController)
        }
    }
}

@Composable
private fun DashboardContent(externalNavController: NavController) {
    val navController = rememberNavController()
    val navBackStackEntry: NavBackStackEntry? by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination
    val showNavBar = currentDestination?.route !in hhRoutes.map { it.route }

    Scaffold(
        bottomBar = { Csm(navController) },
        contentWindowInsets = WindowInsets.navigationBars,
        floatingActionButton = { FabMenu(externalNavController, showNavBar) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = HhRoutes.HhHome.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(HhRoutes.AccountsScreen.route) {
                val vm: AccountsViewModel = koinViewModel()

                val accounts: List<Account> by vm.accounts.collectAsStateWithLifecycle()

                AccountsScreen(
                    accounts = accounts,
                    onCardClick = vm::updateSelected,
                    addAccount = { externalNavController.navigate(HhRoutes.AddAccount.route) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(HhRoutes.HhHome.route) {
                Home()
            }
            composable(HhRoutes.SeeTransaction.route) {
                SeeTransactionsVersionTwo(externalNavController)
            }
        }
    }
}

@Composable
private fun Csm(internalNavController: NavHostController) {
    val navBackStackEntry: NavBackStackEntry? by internalNavController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination

    val showName = currentDestination?.route !in hhRoutes.map { it.route }

    AnimatedVisibility(
        visible = !showName,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
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
}