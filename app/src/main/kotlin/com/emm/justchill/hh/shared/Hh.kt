package com.emm.justchill.hh.shared

import android.content.Context
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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.SessionStatus
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.hh.account.AccountsScreen
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountScreen
import com.emm.justchill.hh.auth.LoginScreen
import com.emm.justchill.hh.auth.LoginViewModel
import com.emm.justchill.hh.auth.SignUpScreen
import com.emm.justchill.hh.auth.SignUpViewModel
import com.emm.justchill.hh.category.AddCategoryScreen
import com.emm.justchill.hh.category.SelectCategoryScreen
import com.emm.justchill.hh.category.SelectCategoryViewModel
import com.emm.justchill.hh.category.SelectIconScreen
import com.emm.justchill.hh.home.HomeScreen
import com.emm.justchill.hh.profile.ProfileScreen
import com.emm.justchill.hh.seetransactions.SeeTransactionsScreen
import com.emm.justchill.hh.transaction.AddTransactionAction
import com.emm.justchill.hh.transaction.AddTransactionScreen
import com.emm.justchill.hh.transaction.AddTransactionViewModel
import com.emm.justchill.hh.transaction.EditTransaction
import com.emm.justchill.hh.transaction.SelectableCategory
import com.emm.justchill.sync.Sync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val START_TAB: BottomBarRoute = SeeTransactionRoute

@Composable
fun Hh() {

    val colors = LocalEmmColors.current
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(START_TAB)
    val resultBus = remember { ResultEventBus() }

    val currentRoute: NavKey? = backStack.lastOrNull()
    val showBottomBar: Boolean = currentRoute is BottomBarRoute

    Scaffold(
        modifier = Modifier.background(colors.bg),
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
        contentWindowInsets = WindowInsets.navigationBars,
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
                entry<PreLoginRoute> {
                    val authRepository = koinInject<AuthRepository>()
                    val ctx: Context? = LocalContext.current.applicationContext

                    LaunchedEffect(Unit) {
                        authRepository.sessionStatus.collect { status ->
                            when (status) {
                                SessionStatus.NotAuthenticated -> backStack.replaceAll(LoginRoute)
                                SessionStatus.Authenticated -> {
                                    ctx?.let(Sync::initialize)
                                    backStack.replaceAll(START_TAB)
                                }
                                SessionStatus.Initializing -> Unit
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                entry<LoginRoute> {
                    val vm: LoginViewModel = koinViewModel()

                    LaunchedEffect(vm.state.successLogin) {
                        if (vm.state.successLogin) {
                            backStack.replaceAll(START_TAB)
                        }
                    }

                    LoginScreen(
                        modifier = Modifier,
                        state = vm.state,
                        onAction = vm::onAction,
                        navigateToRegister = { backStack.add(RegisterRoute) },
                    )
                }

                entry<RegisterRoute> {
                    val vm: SignUpViewModel = koinViewModel()
                    SignUpScreen(
                        state = vm.state,
                        onAction = vm::onAction,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<HomeRoute> {
                    HomeScreen(
                        navigateToAll = dropUnlessResumed {
                            backStack.switchTab(SeeTransactionRoute)
                        },
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
                    val accounts: List<Account> by vm.accounts.collectAsStateWithLifecycle()

                    AccountsScreen(
                        accounts = accounts,
                        addCategory = { backStack.add(CategoryRoute) },
                        addAccount = { backStack.add(AddAccountRoute) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                entry<ProfileRoute> {
                    val authRepository = koinInject<AuthRepository>()
                    val scope: CoroutineScope = rememberCoroutineScope()

                    ProfileScreen(
                        onLogout = {
                            scope.launch {
                                authRepository.logout()
                                backStack.replaceAll(PreLoginRoute)
                            }
                        },
                    )
                }

                entry<AddTransactionRoute> {
                    val vm: AddTransactionViewModel = koinViewModel()

                    ResultEffect<SelectableCategory>(resultBus) { selectableCategory ->
                        resultBus.removeResult<SelectableCategory>()
                        vm.onAction(AddTransactionAction.OnNewValueFromOthers(selectableCategory))
                    }

                    AddTransactionScreen(
                        vm = vm,
                        popBackStack = { backStack.removeLastOrNull() },
                        onOtherCategorySelected = { backStack.add(SelectCategoryRoute) },
                    )
                }

                entry<EditTransactionRoute> { key ->
                    EditTransaction(
                        transactionId = key.transactionId,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<CategoryRoute> {
                    AddCategoryScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onSelectIcon = { backStack.add(SelectIconRoute) },
                    )
                }

                entry<AddAccountRoute> {
                    AddAccountScreen(
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<SelectCategoryRoute> {
                    val vm: SelectCategoryViewModel = koinViewModel()
                    SelectCategoryScreen(
                        onCategorySelected = {
                            resultBus.sendResult(result = it)
                            backStack.removeLastOrNull()
                        },
                        onBack = { backStack.removeLastOrNull() },
                        onValueChange = vm::updateQuery,
                        onNewCategory = { backStack.add(CategoryRoute) },
                        value = vm.state.query,
                        income = vm.state.filteredIncomes,
                        expense = vm.state.filteredExpenses,
                    )
                }

                entry<SelectIconRoute> {
                    SelectIconScreen(
                        onBack = { backStack.removeLastOrNull() },
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
                tint = MaterialTheme.colorScheme.primaryContainer,
            )
        },
        label = {
            Text(
                text = "Agregar",
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primaryContainer,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
            )
        },
    )
}
