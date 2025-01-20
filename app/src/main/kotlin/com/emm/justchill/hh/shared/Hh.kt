package com.emm.justchill.hh.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhOnBackgroundColor
import com.emm.justchill.hh.account.domain.AccountRepository
import com.emm.justchill.hh.account.presentation.Account
import com.emm.justchill.hh.category.presentation.Category
import com.emm.justchill.hh.fasttransaction.Accounts
import com.emm.justchill.hh.fasttransaction.FastTransaction
import com.emm.justchill.hh.fasttransaction.FastTransactionViewModel
import com.emm.justchill.hh.home.Home
import com.emm.justchill.hh.shared.seetransactions.SeeTransactionsVersionTwo
import com.emm.justchill.hh.shared.shared.Account
import com.emm.justchill.hh.shared.shared.Category
import com.emm.justchill.hh.shared.shared.EditTransaction
import com.emm.justchill.hh.shared.shared.FastTransaction
import com.emm.justchill.hh.transaction.presentation.EditTransaction
import com.emm.justchill.hh.transaction.presentation.Transaction
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun Hh() {

    val navController = rememberNavController()

    Scaffold(bottomBar = { Csm(navController) }) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = HhRoutes.HnNewHome.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(HhRoutes.HnNewHome.route) {
                val repository: AccountRepository = koinInject()
                val accounts: List<com.emm.justchill.hh.account.domain.Account> by repository.retrieve()
                    .collectAsStateWithLifecycle(emptyList())
                Accounts(
                    accounts = accounts,
                    onCardClick = { account, transactionType ->
                        navController.navigate(FastTransaction(account.accountId, transactionType))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable<FastTransaction> {
                val fastTransaction: FastTransaction = it.toRoute<FastTransaction>()
                val vm: FastTransactionViewModel = koinViewModel()

                FastTransaction(
                    transactionType = fastTransaction.transactionType,
                    amountValue = vm.amount,
                    onAmountChange = vm::updateAmount,
                    description = vm.description,
                    onDescriptionChange = vm::updateDescription,
                    isEnabledButton = vm.isEnabled,
                    addTransaction = {
                        vm.addTransaction(
                            accountId = fastTransaction.accountId,
                            type = fastTransaction.transactionType
                        )
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
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
        containerColor = HhBackgroundColor,
        contentColor = HhOnBackgroundColor,
    ) {
        hhRoutes.forEach { screen ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HhOnBackgroundColor,
                    selectedTextColor = HhOnBackgroundColor,
                    unselectedTextColor = HhOnBackgroundColor.copy(0.5f),
                    unselectedIconColor = HhOnBackgroundColor.copy(0.5f),
                    indicatorColor = Color.Transparent
                ),
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
                    )
                },
                label = {
                    Text(
                        text = screen.name,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.SansSerif,
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