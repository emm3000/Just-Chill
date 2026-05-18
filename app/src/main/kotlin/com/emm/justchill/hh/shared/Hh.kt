package com.emm.justchill.hh.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emm.domain.account.Account
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.hh.account.AccountsScreen
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountScreen
import com.emm.justchill.hh.category.AddCategoryScreen
import com.emm.justchill.hh.category.SelectCategoryIntent
import com.emm.justchill.hh.category.SelectCategoryScreen
import com.emm.justchill.hh.category.SelectCategoryViewModel
import com.emm.justchill.hh.home.HomeScreen
import com.emm.justchill.hh.profile.ProfileScreen
import com.emm.justchill.hh.report.ReportScreen
import com.emm.justchill.hh.seetransactions.SeeTransactionsScreen
import com.emm.justchill.hh.transaction.AddTransactionIntent
import com.emm.justchill.hh.transaction.AddTransactionScreen
import com.emm.justchill.hh.transaction.AddTransactionViewModel
import com.emm.justchill.hh.transaction.EditTransaction
import com.emm.justchill.hh.transaction.SelectableCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private val START_TAB: BottomBarRoute = SeeTransactionRoute

@Composable
fun Hh() {

    val colors = LocalEmmColors.current
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(START_TAB)
    var pendingCategory by remember { mutableStateOf<SelectableCategory?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val rootScope = rememberCoroutineScope()
    val showRootMessage: (String) -> Unit = { message ->
        rootScope.launch { snackbarHostState.showSnackbar(message) }
    }

    val currentRoute: NavKey? = backStack.lastOrNull()
    val showBottomBar: Boolean = currentRoute is BottomBarRoute

    Scaffold(
        modifier = Modifier.background(colors.bg),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
            ) {
                HhBottomBar(
                    current = currentRoute as? BottomBarRoute,
                    onTabClick = { tab -> backStack.switchTab(tab) },
                    onAddClick = { backStack.add(AddTransactionRoute) },
                )
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->

        NavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .padding(padding),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    HomeScreen(
                        navigateToAll = dropUnlessResumed {
                            backStack.switchTab(SeeTransactionRoute)
                        },
                        navigateToReport = { backStack.add(ReportRoute) },
                    )
                }

                entry<SeeTransactionRoute> {
                    SeeTransactionsScreen(
                        onEditTransaction = { id ->
                            backStack.add(EditTransactionRoute(id))
                        },
                    )
                }

                entry<AccountsRoute> {
                    val vm: AccountsViewModel = koinViewModel()
                    val accountsState by vm.state.collectAsStateWithLifecycle()

                    AccountsScreen(
                        accounts = accountsState.accounts,
                        addCategory = { backStack.add(CategoryRoute()) },
                        addAccount = { backStack.add(AddAccountRoute) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                entry<ProfileRoute> {
                    ProfileScreen(
                        onCategoriesClick = { backStack.add(CategoryRoute()) },
                        onAccountsClick = { backStack.add(AccountsRoute) },
                    )
                }

                entry<ReportRoute> {
                    ReportScreen(
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<AddTransactionRoute> {
                    val vm: AddTransactionViewModel = koinViewModel()

                    LaunchedEffect(pendingCategory) {
                        pendingCategory?.let { selectableCategory ->
                            vm.onIntent(AddTransactionIntent.OnNewValueFromOthers(selectableCategory))
                            pendingCategory = null
                        }
                    }

                    AddTransactionScreen(
                        vm = vm,
                        popBackStack = { backStack.removeLastOrNull() },
                        onOtherCategorySelected = { backStack.add(SelectCategoryRoute) },
                        snackbarHostState = snackbarHostState,
                    )
                }

                entry<EditTransactionRoute> { key ->
                    EditTransaction(
                        transactionId = key.transactionId,
                        onBack = { backStack.removeLastOrNull() },
                        snackbarHostState = snackbarHostState,
                    )
                }

                entry<CategoryRoute> { key ->
                    AddCategoryScreen(
                        onBack = { backStack.removeLastOrNull() },
                        snackbarHostState = snackbarHostState,
                        showSuccessMessage = showRootMessage,
                        vm = koinViewModel(parameters = { parametersOf(key.initialType) }),
                    )
                }

                entry<AddAccountRoute> {
                    AddAccountScreen(
                        onBack = { backStack.removeLastOrNull() },
                        snackbarHostState = snackbarHostState,
                    )
                }

                entry<SelectCategoryRoute> {
                    val vm: SelectCategoryViewModel = koinViewModel()
                    val selectState by vm.state.collectAsStateWithLifecycle()
                    SelectCategoryScreen(
                        onCategorySelected = {
                            pendingCategory = it
                            backStack.removeLastOrNull()
                        },
                        onBack = { backStack.removeLastOrNull() },
                        onValueChange = { vm.onIntent(SelectCategoryIntent.UpdateQuery(it)) },
                        onNewCategory = { backStack.add(CategoryRoute()) },
                        value = selectState.query,
                        income = selectState.filteredIncomes,
                        expense = selectState.filteredExpenses,
                    )
                }

            },
        )
    }
}

/**
 * Replaces the entire back stack with [route]. Used for auth transitions and logout.
 */
private fun NavBackStack<NavKey>.replaceAll(route: NavKey) {
    clear()
    add(route)
}

/**
 * Switches to a bottom-bar tab using the "exit through home" pattern:
 * back stack always starts at [START_TAB], with the selected tab on top (if different).
 */
private fun NavBackStack<NavKey>.switchTab(target: BottomBarRoute) {
    clear()
    add(START_TAB)
    if (target != START_TAB) add(target)
}

@Composable
private fun HhBottomBar(
    current: BottomBarRoute?,
    onTabClick: (BottomBarRoute) -> Unit,
    onAddClick: () -> Unit,
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        val entries = TOP_LEVEL_ROUTES.entries.toList()
        // Insert the "Add" pseudo-item between SeeTransactionRoute and AccountsRoute (position 2)
        entries.forEachIndexed { index, (route, item) ->
            if (index == 2) {
                AddBottomBarItem(onClick = onAddClick)
            }
            val isSelected = route == current
            NavigationBarItem(
                selected = isSelected,
                onClick = dropUnlessResumed { onTabClick(route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        text = item.name,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = LocalContentColor.current,
                        fontFamily = LatoFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}

@Composable
private fun RowScope.AddBottomBarItem(onClick: () -> Unit) {
    NavigationBarItem(
        selected = false,
        onClick = dropUnlessResumed(block = onClick),
        icon = {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Agregar",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        label = {
            Text(
                text = "Agregar",
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
            )
        },
    )
}
