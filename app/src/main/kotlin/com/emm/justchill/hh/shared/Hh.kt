package com.emm.justchill.hh.shared

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpdateRepository
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.account.AddAccountScreen
import com.emm.justchill.hh.category.CategoryScreen
import com.emm.justchill.hh.fasttransaction.AccountsScreen
import com.emm.justchill.hh.fasttransaction.FastTransactionScreen
import com.emm.justchill.hh.fasttransaction.FastTransactionViewModel
import com.emm.justchill.hh.home.Home
import com.emm.justchill.hh.seetransactions.SeeTransactionsVersionTwo
import com.emm.justchill.hh.shared.shared.CategoryRoute
import com.emm.justchill.hh.shared.shared.EditTransactionRoute
import com.emm.justchill.hh.shared.shared.FastTransactionRoute
import com.emm.justchill.hh.transaction.EditTransaction
import com.emm.justchill.hh.transaction.TransactionScreen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun Hh() {

    val navController = rememberNavController()

    Scaffold(
        bottomBar = { Csm(navController) },
        contentWindowInsets = WindowInsets.navigationBars
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = HhRoutes.HhHome.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(HhRoutes.HnNewHome.route) {
                val repository: AccountRepository = koinInject()
                val updateRepository: AccountUpdateRepository = koinInject()
                val coroutineScope = rememberCoroutineScope()
                val accounts: List<com.emm.domain.account.Account> by repository.retrieve().collectAsStateWithLifecycle(emptyList())

                AccountsScreen(
                    accounts = accounts,
                    onCardClick = { account ->
                        coroutineScope.launch {
                            updateRepository.updateSelected(account.accountId)
                        }
                    },
                    addAccount = {
                        navController.navigate(HhRoutes.AddAccount.route)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable<FastTransactionRoute> {
                val fastTransactionRoute: FastTransactionRoute = it.toRoute<FastTransactionRoute>()
                val vm: FastTransactionViewModel = koinViewModel()

                FastTransactionScreen(
                    transactionType = fastTransactionRoute.transactionType,
                    transactionId = fastTransactionRoute.accountId,
                    state = vm.state,
                    onAction = vm::onAction,
                    popBackStack = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(HhRoutes.HhHome.route) {
                Home()
            }
            composable(HhRoutes.AddTransaction.route) {
                TransactionScreen {
                    navController.navigate(HhRoutes.SeeTransaction.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                }
            }
            composable(HhRoutes.SeeTransaction.route) {
                SeeTransactionsVersionTwo(navController)
            }
            composable<EditTransactionRoute> {
                val editTransactionRoute: EditTransactionRoute = it.toRoute<EditTransactionRoute>()
                EditTransaction(navController, editTransactionRoute.transactionId)
            }
            composable(HhRoutes.AddAccount.route) {
                AddAccountScreen(navController)
            }
            composable<CategoryRoute> {
                CategoryScreen(navController)
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