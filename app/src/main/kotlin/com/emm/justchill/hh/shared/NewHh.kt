package com.emm.justchill.hh.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.justchill.core.theme.EmmTheme
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun NewHh(modifier: Modifier = Modifier) {

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RectangleShape
            ) {
                val navBackStackEntry: NavBackStackEntry? by navController.currentBackStackEntryAsState()
                val currentDestination: NavDestination? = navBackStackEntry?.destination

                if (currentDestination?.route !in hhRoutes.map { it.route }) return@ModalDrawerSheet

                ModalDrawerSheetHh {
                    navController.navigate(it) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    coroutineScope.launch {
                        drawerState.close()
                    }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = HhRoutes.HnNewHome.route,
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
private fun ModalDrawerSheetHh(navigateToRoute: (String) -> Unit) {

    ModalDrawerSheet {
        hhRoutes.forEach { screen ->
            DrawerItem(
                text = screen.name,
                icon = screen.icon,
                navigateToRoute = { navigateToRoute(screen.route) },
                modifier = Modifier,
            )
        }
    }
}

@Composable
fun DrawerItem(
    text: String,
    icon: ImageVector,
    navigateToRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clickable { navigateToRoute() },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Text(
            text = text,
        )
    }

}

@Preview
@Composable
private fun ModalDrawerSheetHhPreview() {
    EmmTheme {
        ModalDrawerSheetHh {

        }
    }
}