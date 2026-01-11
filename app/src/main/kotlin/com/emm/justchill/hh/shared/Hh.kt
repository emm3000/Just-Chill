package com.emm.justchill.hh.shared

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.account.AddAccountScreen
import com.emm.justchill.hh.auth.LoginScreen
import com.emm.justchill.hh.auth.LoginViewModel
import com.emm.justchill.hh.auth.SignUpScreen
import com.emm.justchill.hh.auth.SignUpViewModel
import com.emm.justchill.hh.category.CategoryScreen
import com.emm.justchill.hh.fasttransaction.AccountsScreen
import com.emm.justchill.hh.fasttransaction.AccountsViewModel
import com.emm.justchill.hh.home.HomeScreen
import com.emm.justchill.hh.profile.ProfileScreen
import com.emm.justchill.hh.seetransactions.SeeTransactionsVersionTwo
import com.emm.justchill.hh.transaction.EditTransaction
import com.emm.justchill.hh.transaction.TransactionScreen
import com.emm.justchill.sync.Sync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun Hh() {

    val navBackStack: NavBackStack<NavKey> = rememberNavBackStack(PreLoginRoute)

    NavDisplay(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        backStack = navBackStack,
        onBack = { navBackStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<PreLoginRoute> {
                val authRepository: AuthRepository = koinInject<AuthRepository>()

                val ctx: Context? = LocalContext.current.applicationContext

                LaunchedEffect(Unit) {
                    authRepository.sessionStatus.collect { sessionStatus ->
                        when (sessionStatus) {
                            SessionStatus.NotAuthenticated -> {
                                navBackStack.removeLastOrNull()
                                navBackStack.add(LoginRoute)
                            }

                            SessionStatus.Initializing -> {}
                            SessionStatus.Authenticated -> {
                                ctx?.let(Sync::initialize)
                                navBackStack.removeLastOrNull()
                                navBackStack.add(DashboardRoute)
                            }
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
                        navBackStack.removeLastOrNull()
                        navBackStack.add(DashboardRoute)
                    }
                }
                LoginScreen(
                    modifier = Modifier,
                    state = vm.state,
                    onAction = vm::onAction,
                    navigateToRegister = { navBackStack.add(RegisterRoute) }
                )
            }
            entry<RegisterRoute> {
                val vm: SignUpViewModel = koinViewModel()

                SignUpScreen(
                    state = vm.state,
                    onAction = vm::onAction,
                    onBack = { navBackStack.removeLastOrNull() }
                )
            }
            entry<DashboardRoute> {
                DashboardContent(navBackStack)
            }
            entry<AddTransactionRoute> {
                TransactionScreen(
                    popBackStack = { navBackStack.removeLastOrNull() }
                )
            }
            entry<EditTransactionRoute> {
                EditTransaction(navBackStack, it.transactionId)
            }
            entry<CategoryRoute> {
                CategoryScreen(navBackStack)
            }
            entry<AddAccountRoute> {
                AddAccountScreen(navBackStack)
            }
        },
    )
}

@Composable
fun DashboardContent(externalNavBack: NavBackStack<NavKey>) {

    val navigationState: NavigationState = rememberNavigationState(
        startRoute = HomeRoute,
        topLevelRoutes = TOP_LEVEL_ROUTES.keys,
    )

    val navigator: Navigator = remember { Navigator(navigationState) }

    val entryProvider = entryProvider {
        entry<HomeRoute> {
            HomeScreen(
                navigateToAll = { navigator.navigate(SeeTransactionRoute) }
            )
        }
        entry<AccountsRoute> {
            val vm: AccountsViewModel = koinViewModel()

            val accounts: List<Account> by vm.accounts.collectAsStateWithLifecycle()

            AccountsScreen(
                accounts = accounts,
                state = vm.state,
                onAction = vm::onAction,
                addCategory = {
                    externalNavBack.add(CategoryRoute)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        entry<SeeTransactionRoute> {
            SeeTransactionsVersionTwo(externalNavBack)
        }
        entry<ProfileRoute> {
            val authRepository = koinInject<AuthRepository>()
            val scope: CoroutineScope = rememberCoroutineScope()

            ProfileScreen(
                onLogout = {
                    scope.launch {
                        authRepository.logout()
                        externalNavBack.removeLastOrNull()
                        externalNavBack.add(PreLoginRoute)
                    }
                }
            )
        }
    }

    Scaffold(
        bottomBar = { Csm(navigationState, navigator, externalNavBack) },
        contentWindowInsets = WindowInsets.navigationBars,
    ) { paddingValues ->
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun Csm(
    navigationState: NavigationState,
    navigator: Navigator,
    externalNavBack: NavBackStack<NavKey>,
) {

    val navItems: Map<NavKey, HhNavBarItem> = remember {
        val newRouteItem = HhNavBarItem(name = "Agregar", route = "class", icon = Icons.Filled.Add)
        val routesList: MutableList<Pair<NavKey, HhNavBarItem>> = TOP_LEVEL_ROUTES.toList().toMutableList()
        routesList.add(2, Pair(AddTransactionRoute, newRouteItem))
        routesList.toMap()
    }

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        navItems.forEach { (key: NavKey, value: HhNavBarItem) ->
            val isSelected = key == navigationState.topLevelRoute
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (key == AddTransactionRoute) {
                        externalNavBack.add(key)
                    } else {
                        navigator.navigate(key)
                    }
                },
                icon = {
                    Icon(
                        imageVector = value.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (key == AddTransactionRoute) MaterialTheme.colorScheme.primaryContainer else LocalContentColor.current
                    )
                },
                label = {
                    Text(
                        text = value.name,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (key == AddTransactionRoute) MaterialTheme.colorScheme.primaryContainer else LocalContentColor.current,
                        fontFamily = LatoFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}