package com.emm.justchill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.preferences.AppPreferences
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.EmmSnackbarHost
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.account.AccountsEffect
import com.emm.justchill.hh.account.AccountsScreen
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountScreen
import com.emm.justchill.hh.category.AddCategoryScreen
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.CategoriesEffect
import com.emm.justchill.hh.category.CategoriesScreen
import com.emm.justchill.hh.category.CategoriesViewModel
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.auth.AuthScreen
import com.emm.justchill.hh.home.HomeEffect
import com.emm.justchill.hh.home.HomeScreen
import com.emm.justchill.hh.home.HomeViewModel
import com.emm.justchill.hh.onboarding.ManifestoScreen
import com.emm.justchill.hh.profile.ProfileEffect
import com.emm.justchill.hh.profile.ProfileIntent
import com.emm.justchill.hh.profile.ProfileScreen
import com.emm.justchill.hh.profile.ProfileViewModel
import com.emm.justchill.hh.recurring.AddEditRecurringMovementScreen
import com.emm.justchill.hh.recurring.RecurringMovementsEffect
import com.emm.justchill.hh.recurring.RecurringMovementsScreen
import com.emm.justchill.hh.recurring.RecurringMovementsViewModel
import com.emm.justchill.hh.report.ReportScreen
import com.emm.justchill.hh.seetransactions.SeeTransactionsScreen
import com.emm.justchill.hh.shared.AccountsRoute
import com.emm.justchill.hh.shared.AddAccountRoute
import com.emm.justchill.hh.shared.AddEditRecurringMovementRoute
import com.emm.justchill.hh.shared.AddTransactionRoute
import com.emm.justchill.hh.shared.AuthRoute
import com.emm.justchill.hh.shared.BottomBarRoute
import com.emm.justchill.hh.shared.CategoriesListRoute
import com.emm.justchill.hh.shared.CategoryRoute
import com.emm.justchill.hh.shared.EditTransactionRoute
import com.emm.justchill.hh.shared.HhBottomBar
import com.emm.justchill.hh.shared.HomeRoute
import com.emm.justchill.hh.shared.ManifestoRoute
import com.emm.justchill.hh.shared.ProfileRoute
import com.emm.justchill.hh.shared.RecurringMovementsRoute
import com.emm.justchill.hh.shared.ReportRoute
import com.emm.justchill.hh.shared.SeeTransactionRoute
import com.emm.justchill.hh.shared.SyncEventsHandler
import com.emm.justchill.hh.shared.popToTransactionScreen
import com.emm.justchill.hh.shared.switchTab
import com.emm.justchill.hh.shared.toText
import com.emm.justchill.hh.transaction.AddTransactionIntent
import com.emm.justchill.hh.transaction.AddTransactionScreen
import com.emm.justchill.hh.transaction.AddTransactionViewModel
import com.emm.justchill.hh.transaction.EditTransaction
import com.emm.justchill.hh.transaction.SelectableCategory
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// iOS nav host. Mirrors the Android Hh.kt structure (bottom nav + center "Agregar" + NavDisplay
// entryProvider) for the LOCAL-FIRST subset only, using the JetBrains Compose Multiplatform
// navigation3 port. Lives in iosMain — Android keeps its own androidx.navigation3 Hh.kt (Option A).
// The route keys (HhRoutes.kt) + bottom bar (HhBottomBar.kt) + ProfileMessage.toText are shared from
// commonMain; only the NavDisplay/entryProvider body stays platform-specific (nav3-UI dependency split).
//
// First launch shows the Manifesto gate (mirrors Android); subsequent launches land on Home. The
// "Agregar" center button pushes AddTransaction. Platform callbacks (share/export/import) are still
// no-oped on iOS.

private val START_TAB: BottomBarRoute = HomeRoute

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun IosApp() {
    EmmTheme {
        val colors = LocalEmmColors.current
        val appPrefs: AppPreferences = koinInject()
        val syncController: SyncController = koinInject()
        // First-launch Manifesto gate (mirrors Android Hh.kt): show the manifesto once, then land on
        // START_TAB on every subsequent launch.
        val startRoute: NavKey = remember {
            if (appPrefs.firstLaunchSeen) START_TAB else ManifestoRoute()
        }
        // KMP rememberNavBackStack needs a SavedStateConfiguration whose serializersModule registers
        // every NavKey subtype (Kotlin/Native has no reflection-based serializer discovery like
        // Android). DEFAULT carries an empty module and crashes at runtime; iosNavSavedStateConfiguration
        // (IosRoutes.kt) wires the open NavKey polymorphism.
        val backStack: NavBackStack<NavKey> =
            rememberNavBackStack(iosNavSavedStateConfiguration, startRoute)
        var pendingCategory by remember { mutableStateOf<SelectableCategory?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val rootScope = rememberCoroutineScope()
        val showRootMessage: (String) -> Unit = { message ->
            rootScope.launch { snackbarHostState.showEmmSnackbar(message) }
        }

        // Collects one-shot sync events from the SAME SyncController instance the orchestrator emits
        // on (KoinIos binds single<SyncController> { get<SyncOrchestrator>() } and start()s it). Shows
        // the manual-sync retry snackbar and the session-expired snackbar — parity with Android.
        SyncEventsHandler(
            syncController = syncController,
            snackbarHostState = snackbarHostState,
            // Guard: only push AuthRoute if it is not anywhere in the back stack.
            onNavigateToSignIn = { if (backStack.none { it is AuthRoute }) backStack.add(AuthRoute) },
        )

        val currentRoute: NavKey? = backStack.lastOrNull()
        val showBottomBar: Boolean = currentRoute is BottomBarRoute

        Scaffold(
            modifier = Modifier.background(colors.bg),
            snackbarHost = { EmmSnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                    exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
                ) {
                    HhBottomBar(
                        current = currentRoute as? BottomBarRoute,
                        onTabClick = { tab -> backStack.switchTab(tab, START_TAB) },
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
                    entry<ManifestoRoute> { key ->
                        ManifestoScreen(
                            isRevisit = key.isRevisit,
                            onStart = {
                                if (key.isRevisit) {
                                    backStack.removeLastOrNull()
                                } else {
                                    appPrefs.firstLaunchSeen = true
                                    backStack.replaceAll(START_TAB)
                                }
                            },
                        )
                    }

                    entry<HomeRoute> {
                        IosHomeEntry(
                            navigateToAll = { backStack.switchTab(SeeTransactionRoute, START_TAB) },
                            navigateToAdd = { backStack.add(AddTransactionRoute) },
                            navigateToEdit = { id -> backStack.add(EditTransactionRoute(id)) },
                            navigateToReport = { backStack.add(ReportRoute) },
                            snackbarHostState = snackbarHostState,
                        )
                    }

                    entry<SeeTransactionRoute> {
                        SeeTransactionsScreen(
                            onEditTransaction = { id -> backStack.add(EditTransactionRoute(id)) },
                        )
                    }

                    entry<AccountsRoute> {
                        val vm: AccountsViewModel = koinViewModel()
                        val state by vm.state.collectAsStateWithLifecycle()
                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is AccountsEffect.ShowMessage -> showRootMessage(effect.text)
                                }
                            }
                        }
                        AccountsScreen(
                            state = state,
                            onIntent = vm::onIntent,
                            addCategory = { backStack.add(CategoryRoute()) },
                            addAccount = { backStack.add(AddAccountRoute) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    entry<ProfileRoute> {
                        val vm: ProfileViewModel = koinViewModel()
                        val state by vm.state.collectAsStateWithLifecycle()
                        // Surface sign-out / delete-account outcomes (and errors) via the root
                        // snackbar — mirrors Android's ProfileRoute effect collection.
                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is ProfileEffect.ShowError -> showRootMessage(effect.error.toUserMessage())
                                    is ProfileEffect.Notify -> showRootMessage(effect.message.toText())
                                    // Export/import (SAF) is phase 6b on iOS — onExportClick never fires,
                                    // so ExportReady is unreachable here; ignore it for now.
                                    is ProfileEffect.ExportReady -> Unit
                                }
                            }
                        }
                        ProfileScreen(
                            state = state,
                            appVersion = "1.0.0",
                            isDebug = false,
                            onCategoriesClick = { backStack.add(CategoriesListRoute) },
                            onAccountsClick = { backStack.add(AccountsRoute) },
                            onRecurringClick = { backStack.add(RecurringMovementsRoute) },
                            onAboutClick = { backStack.add(ManifestoRoute(isRevisit = true)) },
                            // Backup is phase 6b on iOS — no-op (must not crash).
                            onExportClick = { /* TODO phase 6b: iOS export (SAF equivalent) */ },
                            onImportClick = { /* TODO phase 6b: iOS import */ },
                            onPrivacyClick = { /* TODO phase 6+: privacy policy screen */ },
                            // Auth (6a): opt-in from Profile. On success AuthScreen pops back here.
                            onSignInClick = { backStack.add(AuthRoute) },
                            onSignOutClick = { vm.onIntent(ProfileIntent.SignOut) },
                            onDeleteAccountClick = { vm.onIntent(ProfileIntent.DeleteAccount) },
                            // Sync (6b): manual trigger. The Syncing spinner / RetryPill / "última
                            // sincronización" row is driven by SyncController.status in ProfileViewModel,
                            // so it reflects this cycle automatically once SyncOrchestrator is bound.
                            onSyncNowClick = { vm.onIntent(ProfileIntent.SyncNow) },
                        )
                    }

                    entry<AddTransactionRoute> {
                        val vm: AddTransactionViewModel = koinViewModel()
                        LaunchedEffect(pendingCategory) {
                            pendingCategory?.let { selectable ->
                                vm.onIntent(AddTransactionIntent.OnNewValueFromOthers(selectable))
                                pendingCategory = null
                            }
                        }
                        AddTransactionScreen(
                            vm = vm,
                            popBackStack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            onAddNewCategory = {
                                backStack.add(CategoryRoute(propagateToTransaction = true))
                            },
                            onAddNewAccount = { backStack.add(AddAccountRoute) },
                        )
                    }

                    entry<EditTransactionRoute> { key ->
                        EditTransaction(
                            transactionId = key.transactionId,
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            onAddNewAccount = { backStack.add(AddAccountRoute) },
                        )
                    }

                    entry<AddAccountRoute> {
                        AddAccountScreen(
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                        )
                    }

                    entry<CategoriesListRoute> {
                        val vm: CategoriesViewModel = koinViewModel()
                        val state by vm.state.collectAsStateWithLifecycle()
                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is CategoriesEffect.ShowMessage -> showRootMessage(effect.text)
                                }
                            }
                        }
                        CategoriesScreen(
                            state = state,
                            onIntent = vm::onIntent,
                            onAddCategory = { backStack.add(CategoryRoute()) },
                            onBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    entry<CategoryRoute> { key ->
                        AddCategoryScreen(
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            onCategorySave = { created ->
                                if (key.propagateToTransaction) {
                                    pendingCategory = SelectableCategory(
                                        categoryId = created.categoryId,
                                        name = created.name,
                                        icon = AppIconCatalog.findById(created.icon),
                                        color = findById(created.color),
                                        categoryType = created.categoryType,
                                    )
                                    backStack.popToTransactionScreen()
                                } else {
                                    showRootMessage("Categoría «${created.name}» creada")
                                    backStack.removeLastOrNull()
                                }
                            },
                            vm = koinViewModel(
                                parameters = { parametersOf(key.initialType, key.initialName) },
                            ),
                        )
                    }

                    entry<RecurringMovementsRoute> {
                        IosRecurringEntry(
                            onNavigateToAddEdit = { id -> backStack.add(AddEditRecurringMovementRoute(id)) },
                            onShowError = showRootMessage,
                        )
                    }

                    entry<AddEditRecurringMovementRoute> { key ->
                        AddEditRecurringMovementScreen(
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            id = key.id,
                        )
                    }

                    entry<ReportRoute> {
                        ReportScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onAddTransaction = { backStack.add(AddTransactionRoute) },
                            // Sharing is a platform concern — no-op for 5b.
                            onShareText = { /* TODO phase 6b: iOS share sheet */ },
                        )
                    }

                    entry<AuthRoute> {
                        AuthScreen(
                            // On successful sign-in/sign-up AuthViewModel emits NavigateBack, which
                            // calls onBack — popping Auth and returning to Profile (which then shows
                            // the signed-in state via its ObserveSessionUseCase subscription).
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            // Opening the system mail app needs UIKit (UIApplication.openURL with a
                            // mailto: / message:// scheme) — deferred to 6b. No-op for now: the
                            // CheckEmail screen still shows the address and the resend link works.
                            onOpenEmailApp = { /* TODO phase 6b: open iOS Mail app */ },
                            // iOS hides Google Sign-In (deferred post-v1). Email/password only.
                            showGoogleSignIn = false,
                        )
                    }
                },
            )
        }
    }
}

/**
 * Replaces the entire back stack with [route]. Used by the Manifesto first-launch gate to land on
 * START_TAB. Mirrors the Android Hh.kt helper.
 */
private fun NavBackStack<NavKey>.replaceAll(route: NavKey) {
    clear()
    add(route)
}

@Composable
private fun IosHomeEntry(
    navigateToAll: () -> Unit,
    navigateToAdd: () -> Unit,
    navigateToEdit: (String) -> Unit,
    navigateToReport: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val vm: HomeViewModel = koinViewModel()
    var confirmSheetOpen by remember { mutableStateOf(false) }
    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                HomeEffect.CloseConfirmSheet -> confirmSheetOpen = false
                is HomeEffect.ShowError -> snackbarHostState.showEmmSnackbar(effect.message)
            }
        }
    }
    HomeScreen(
        homeViewModel = vm,
        confirmSheetOpen = confirmSheetOpen,
        onConfirmSheetOpenChange = { confirmSheetOpen = it },
        navigateToAll = navigateToAll,
        navigateToAdd = navigateToAdd,
        navigateToEdit = navigateToEdit,
        navigateToReport = navigateToReport,
    )
}

@Composable
private fun IosRecurringEntry(onNavigateToAddEdit: (String?) -> Unit, onShowError: (String) -> Unit) {
    val vm: RecurringMovementsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is RecurringMovementsEffect.NavigateToAddEdit -> onNavigateToAddEdit(effect.id)
                is RecurringMovementsEffect.ShowError -> onShowError(effect.message)
            }
        }
    }
    RecurringMovementsScreen(state = state, onIntent = vm::onIntent)
}
