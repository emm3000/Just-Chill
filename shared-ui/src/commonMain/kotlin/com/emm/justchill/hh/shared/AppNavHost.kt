package com.emm.justchill.hh.shared

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.account.AccountsEffect
import com.emm.justchill.hh.account.AccountsScreen
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountScreen
import com.emm.justchill.hh.auth.AuthScreen
import com.emm.justchill.hh.category.AddCategoryScreen
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.CategoriesEffect
import com.emm.justchill.hh.category.CategoriesScreen
import com.emm.justchill.hh.category.CategoriesViewModel
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.home.HomeEffect
import com.emm.justchill.hh.home.HomeScreen
import com.emm.justchill.hh.home.HomeViewModel
import com.emm.justchill.hh.onboarding.ManifestoScreen
import com.emm.justchill.hh.profile.PrivacyPolicyScreen
import com.emm.justchill.hh.profile.ProfileEffect
import com.emm.justchill.hh.profile.ProfileIntent
import com.emm.justchill.hh.profile.ProfileMessage
import com.emm.justchill.hh.profile.ProfileScreen
import com.emm.justchill.hh.profile.ProfileViewModel
import com.emm.justchill.hh.recurring.AddEditRecurringMovementScreen
import com.emm.justchill.hh.recurring.RecurringMovementsEffect
import com.emm.justchill.hh.recurring.RecurringMovementsScreen
import com.emm.justchill.hh.recurring.RecurringMovementsViewModel
import com.emm.justchill.hh.report.ReportScreen
import com.emm.justchill.hh.seetransactions.SeeTransactionsScreen
import com.emm.justchill.hh.transaction.AddTransactionIntent
import com.emm.justchill.hh.transaction.AddTransactionScreen
import com.emm.justchill.hh.transaction.AddTransactionViewModel
import com.emm.justchill.hh.transaction.EditTransaction
import com.emm.justchill.hh.transaction.SelectableCategory
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

// Single Compose Multiplatform nav host for both Android and iOS. Merges the two former hosts
// (androidApp Hh.kt + iosMain IosApp.kt) into one commonMain composable. The route keys, bottom bar,
// sync-event handling and ProfileMessage copy already live in commonMain; this slice unifies the
// NavDisplay + entryProvider body too. The five capabilities that genuinely differ per platform
// (backup export/import, share, open-email, privacy click + the start tab) sit behind expect/actual
// (PlatformHostActions + startTab) — Android wires real intents, iOS no-ops, exactly as before.
//
// Both platforms now run on the JetBrains Compose Multiplatform navigation3-UI port (NavDisplay) over
// Google's multiplatform navigation3-runtime (NavKey/NavBackStack). EmmTheme is applied here so each
// entry point only calls AppNavHost().

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    EmmTheme {
        val colors = LocalEmmColors.current
        val appPrefs: AppPreferences = koinInject()
        // Inject via the SyncController port so the SAME instance the orchestrator emits on drives the
        // sync-event snackbars. Android binds SyncOrchestrator to SyncController; iOS binds it too.
        val syncController: SyncController = koinInject()
        val appVersion: String = koinInject(named("appVersion"))

        // First-launch Manifesto gate: show the manifesto once, then land on startTab on every
        // subsequent launch.
        val startRoute: NavKey = remember {
            if (appPrefs.firstLaunchSeen) startTab else ManifestoRoute()
        }
        // 2-arg rememberNavBackStack with an explicit SavedStateConfiguration on BOTH platforms (one
        // host, one call). iOS needs it (no K/N reflection serializer discovery); Android tolerates it.
        val backStack: NavBackStack<NavKey> =
            rememberNavBackStack(navSavedStateConfiguration, startRoute)
        // Host-level navigator for the two call sites that compose OUTSIDE NavDisplay: the bottom bar
        // and SyncEventsHandler. Only its duplicate-key guard is live here — see AppNavigator. Every
        // entry below builds its own, in-scene, where the transition guard works too.
        val hostNav: AppNavigator = rememberAppNavigator(backStack)
        var pendingCategory by remember { mutableStateOf<SelectableCategory?>(null) }
        // Holds the JSON contents of an imported backup file until the user confirms the destructive
        // replace. Hoisted to the host so the Android SAF import launcher (created in
        // rememberPlatformHostActions, above NavDisplay) can feed it back; the Profile entry renders
        // the confirmation dialog. Never set on iOS (requestImport no-ops there).
        var pendingImportJson by remember { mutableStateOf<String?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val rootScope = rememberCoroutineScope()
        val showRootMessage: (String) -> Unit = { message ->
            rootScope.launch { snackbarHostState.showEmmSnackbar(message) }
        }
        val platform = rememberPlatformHostActions(
            snackbarHostState = snackbarHostState,
            scope = rootScope,
            onImport = { json -> pendingImportJson = json },
        )

        SyncEventsHandler(
            syncController = syncController,
            snackbarHostState = snackbarHostState,
            onNavigateToSignIn = { hostNav.push(AuthRoute) },
        )

        val currentRoute: NavKey? = backStack.lastOrNull()
        val showBottomBar: Boolean = currentRoute is BottomBarRoute

        Scaffold(
            modifier = modifier.background(colors.bg),
            snackbarHost = { EmmSnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                    exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
                ) {
                    HhBottomBar(
                        current = currentRoute as? BottomBarRoute,
                        onTabClick = { tab -> hostNav.switchTab(tab) },
                        onAddClick = { hostNav.push(AddTransactionRoute) },
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
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<ManifestoRoute> { key ->
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        ManifestoScreen(
                            isRevisit = key.isRevisit,
                            onStart = {
                                if (key.isRevisit) {
                                    nav.pop()
                                } else {
                                    appPrefs.firstLaunchSeen = true
                                    nav.replaceAll(startTab)
                                }
                            },
                        )
                    }

                    entry<PrivacyPolicyRoute> {
                        // Registered on both platforms; iOS never navigates here (privacy click is inert).
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        PrivacyPolicyScreen(
                            onBack = { nav.pop() },
                        )
                    }

                    entry<AuthRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        AuthScreen(
                            onBack = { nav.pop() },
                            snackbarHostState = snackbarHostState,
                            onOpenEmailApp = platform.onOpenEmailApp,
                            showGoogleSignIn = platform.showGoogleSignIn,
                        )
                    }

                    entry<HomeRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        HomeEntry(
                            navigateToAll = { nav.switchTab(SeeTransactionRoute) },
                            navigateToAdd = { nav.push(AddTransactionRoute) },
                            navigateToEdit = { id -> nav.push(EditTransactionRoute(id)) },
                            navigateToReport = { nav.push(ReportRoute) },
                            snackbarHostState = snackbarHostState,
                        )
                    }

                    entry<SeeTransactionRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        SeeTransactionsScreen(
                            onEditTransaction = { id ->
                                nav.push(EditTransactionRoute(id))
                            },
                        )
                    }

                    entry<AccountsRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        val vm: AccountsViewModel = koinViewModel()
                        val accountsState by vm.state.collectAsStateWithLifecycle()

                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is AccountsEffect.ShowMessage -> showRootMessage(effect.text)
                                }
                            }
                        }

                        AccountsScreen(
                            state = accountsState,
                            onIntent = vm::onIntent,
                            addCategory = { nav.push(CategoryRoute()) },
                            addAccount = { nav.push(AddAccountRoute) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    entry<CategoriesListRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        val vm: CategoriesViewModel = koinViewModel()
                        val categoriesState by vm.state.collectAsStateWithLifecycle()

                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is CategoriesEffect.ShowMessage -> showRootMessage(effect.text)
                                }
                            }
                        }

                        CategoriesScreen(
                            state = categoriesState,
                            onIntent = vm::onIntent,
                            onAddCategory = { nav.push(CategoryRoute()) },
                            onBack = { nav.pop() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    entry<ProfileRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        val vm: ProfileViewModel = koinViewModel()
                        val profileState by vm.state.collectAsStateWithLifecycle()

                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is ProfileEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                                        message = effect.error.toUserMessage(),
                                        tone = EmmSnackbarTone.Error,
                                    )

                                    // VM finished generating the backup; the platform layer owns the
                                    // SAF write. No-op on iOS (export is gated off there, so this never
                                    // fires).
                                    is ProfileEffect.ExportReady -> platform.requestExport(effect.json)

                                    is ProfileEffect.Notify -> snackbarHostState.showEmmSnackbar(
                                        message = effect.message.toText(),
                                        tone = when (effect.message) {
                                            ProfileMessage.ExportFailed,
                                            ProfileMessage.ImportFailed,
                                            -> EmmSnackbarTone.Error

                                            else -> EmmSnackbarTone.Success
                                        },
                                    )
                                }
                            }
                        }

                        pendingImportJson?.let { json ->
                            val dialogColors = LocalEmmColors.current
                            AlertDialog(
                                onDismissRequest = { pendingImportJson = null },
                                title = { Text("¿Reemplazar tu data?") },
                                text = { Text("Esto va a borrar todo lo que tengas hoy y poner lo del archivo.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            vm.onIntent(ProfileIntent.ImportJson(json))
                                            pendingImportJson = null
                                        },
                                    ) {
                                        Text(
                                            text = "Reemplazar todo",
                                            color = dialogColors.danger,
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { pendingImportJson = null }) {
                                        Text("Cancelar")
                                    }
                                },
                            )
                        }

                        ProfileScreen(
                            state = profileState,
                            appVersion = appVersion,
                            isDebug = platform.isDebug,
                            onCategoriesClick = { nav.push(CategoriesListRoute) },
                            onAccountsClick = { nav.push(AccountsRoute) },
                            onRecurringClick = { nav.push(RecurringMovementsRoute) },
                            onAboutClick = { nav.push(ManifestoRoute(isRevisit = true)) },
                            onExportClick = {
                                if (platform.supportsBackup) vm.onIntent(ProfileIntent.ExportRequested)
                            },
                            onImportClick = { if (platform.supportsBackup) platform.requestImport() },
                            onPrivacyClick = {
                                if (platform.supportsPrivacyPolicy) nav.push(PrivacyPolicyRoute)
                            },
                            onSignInClick = { nav.push(AuthRoute) },
                            onSignOutClick = { vm.onIntent(ProfileIntent.SignOut) },
                            onDeleteAccountClick = { vm.onIntent(ProfileIntent.DeleteAccount) },
                            onSyncNowClick = { vm.onIntent(ProfileIntent.SyncNow) },
                        )
                    }

                    entry<ReportRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        ReportScreen(
                            onBack = { nav.pop() },
                            onAddTransaction = { nav.push(AddTransactionRoute) },
                            onShareText = platform.onShareText,
                        )
                    }

                    entry<AddTransactionRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        val vm: AddTransactionViewModel = koinViewModel()

                        LaunchedEffect(pendingCategory) {
                            pendingCategory?.let { selectableCategory ->
                                vm.onIntent(AddTransactionIntent.OnNewValueFromOthers(selectableCategory))
                                pendingCategory = null
                            }
                        }

                        AddTransactionScreen(
                            vm = vm,
                            popBackStack = { nav.pop() },
                            snackbarHostState = snackbarHostState,
                            onAddNewCategory = {
                                nav.push(
                                    CategoryRoute(
                                        propagateToTransaction = true,
                                    ),
                                )
                            },
                            onAddNewAccount = {
                                nav.push(AddAccountRoute)
                            },
                        )
                    }

                    entry<EditTransactionRoute> { key ->
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        EditTransaction(
                            transactionId = key.transactionId,
                            onBack = { nav.pop() },
                            snackbarHostState = snackbarHostState,
                            onAddNewAccount = { nav.push(AddAccountRoute) },
                        )
                    }

                    entry<CategoryRoute> { key ->
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        AddCategoryScreen(
                            onBack = { nav.pop() },
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
                                    nav.popToTransaction()
                                } else {
                                    showRootMessage("Categoría «${created.name}» creada")
                                    nav.pop()
                                }
                            },
                            vm = koinViewModel(
                                parameters = { parametersOf(key.initialType, key.initialName) },
                            ),
                        )
                    }

                    entry<AddAccountRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        AddAccountScreen(
                            onBack = { nav.pop() },
                            snackbarHostState = snackbarHostState,
                        )
                    }

                    entry<RecurringMovementsRoute> {
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        RecurringMovementsEntry(
                            onNavigateToAddEdit = { id -> nav.push(AddEditRecurringMovementRoute(id)) },
                            onShowError = showRootMessage,
                        )
                    }

                    entry<AddEditRecurringMovementRoute> { key ->
                        val nav: AppNavigator = rememberAppNavigator(backStack)
                        AddEditRecurringMovementScreen(
                            onBack = { nav.pop() },
                            snackbarHostState = snackbarHostState,
                            id = key.id,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeEntry(
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
                is HomeEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
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
private fun RecurringMovementsEntry(onNavigateToAddEdit: (String?) -> Unit, onShowError: (String) -> Unit) {
    val vm: RecurringMovementsViewModel = koinViewModel()
    val recurringState by vm.state.collectAsStateWithLifecycle()
    val currentNavigate by rememberUpdatedState(onNavigateToAddEdit)
    val currentShowError by rememberUpdatedState(onShowError)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is RecurringMovementsEffect.NavigateToAddEdit -> currentNavigate(effect.id)
                is RecurringMovementsEffect.ShowError -> currentShowError(effect.message)
            }
        }
    }

    RecurringMovementsScreen(state = recurringState, onIntent = vm::onIntent)
}
