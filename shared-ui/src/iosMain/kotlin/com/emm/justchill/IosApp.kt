package com.emm.justchill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// iOS nav host. Mirrors the Android Hh.kt structure (bottom nav + center "Agregar" + NavDisplay
// entryProvider) for the LOCAL-FIRST subset only, using the JetBrains Compose Multiplatform
// navigation3 port. Lives in iosMain — Android keeps its own androidx.navigation3 Hh.kt (Option A).
//
// Launches on Home (no Manifesto/Auth gate — phase 6). The "Agregar" center button pushes
// AddTransaction. Platform callbacks (share/export/import/sign-in) are no-oped for 5b.

private val START_TAB: IosBottomBarRoute = IosHomeRoute

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun IosApp() {
    EmmTheme {
        val colors = LocalEmmColors.current
        // KMP rememberNavBackStack needs a SavedStateConfiguration whose serializersModule registers
        // every NavKey subtype (Kotlin/Native has no reflection-based serializer discovery like
        // Android). DEFAULT carries an empty module and crashes at runtime; iosNavSavedStateConfiguration
        // (IosRoutes.kt) wires the open NavKey polymorphism.
        val backStack: NavBackStack<NavKey> =
            rememberNavBackStack(iosNavSavedStateConfiguration, START_TAB)
        var pendingCategory by remember { mutableStateOf<SelectableCategory?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val rootScope = rememberCoroutineScope()
        val showRootMessage: (String) -> Unit = { message ->
            rootScope.launch { snackbarHostState.showEmmSnackbar(message) }
        }

        val currentRoute: NavKey? = backStack.lastOrNull()
        val showBottomBar: Boolean = currentRoute is IosBottomBarRoute

        Scaffold(
            modifier = Modifier.background(colors.bg),
            snackbarHost = { EmmSnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                    exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
                ) {
                    IosBottomBar(
                        current = currentRoute as? IosBottomBarRoute,
                        onTabClick = { tab -> backStack.switchTab(tab) },
                        onAddClick = { backStack.add(IosAddTransactionRoute) },
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
                    entry<IosHomeRoute> {
                        IosHomeEntry(
                            navigateToAll = { backStack.switchTab(IosSeeTransactionsRoute) },
                            navigateToAdd = { backStack.add(IosAddTransactionRoute) },
                            navigateToEdit = { id -> backStack.add(IosEditTransactionRoute(id)) },
                            navigateToReport = { backStack.add(IosReportRoute) },
                            snackbarHostState = snackbarHostState,
                        )
                    }

                    entry<IosSeeTransactionsRoute> {
                        SeeTransactionsScreen(
                            onEditTransaction = { id -> backStack.add(IosEditTransactionRoute(id)) },
                        )
                    }

                    entry<IosAccountsRoute> {
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
                            addCategory = { backStack.add(IosAddCategoryRoute()) },
                            addAccount = { backStack.add(IosAddAccountRoute) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    entry<IosProfileRoute> {
                        val vm: ProfileViewModel = koinViewModel()
                        val state by vm.state.collectAsStateWithLifecycle()
                        // Surface sign-out / delete-account outcomes (and errors) via the root
                        // snackbar — mirrors Android's ProfileRoute effect collection.
                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is ProfileEffect.ShowError -> showRootMessage(effect.error.toUserMessage())
                                    is ProfileEffect.Notify -> showRootMessage(effect.message.toIosText())
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
                            onCategoriesClick = { backStack.add(IosCategoriesListRoute) },
                            onAccountsClick = { backStack.add(IosAccountsRoute) },
                            onRecurringClick = { backStack.add(IosRecurringRoute) },
                            onAboutClick = { /* TODO phase 6+: Manifesto/About on iOS */ },
                            // Backup is phase 6b on iOS — no-op (must not crash).
                            onExportClick = { /* TODO phase 6b: iOS export (SAF equivalent) */ },
                            onImportClick = { /* TODO phase 6b: iOS import */ },
                            onPrivacyClick = { /* TODO phase 6+: privacy policy screen */ },
                            // Auth (6a): opt-in from Profile. On success AuthScreen pops back here.
                            onSignInClick = { backStack.add(IosAuthRoute) },
                            onSignOutClick = { vm.onIntent(ProfileIntent.SignOut) },
                            onDeleteAccountClick = { vm.onIntent(ProfileIntent.DeleteAccount) },
                            // Sync is phase 6b — the no-op SyncController absorbs this harmlessly.
                            onSyncNowClick = { /* TODO phase 6b: sync on iOS */ },
                        )
                    }

                    entry<IosAddTransactionRoute> {
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
                                backStack.add(IosAddCategoryRoute(propagateToTransaction = true))
                            },
                            onAddNewAccount = { backStack.add(IosAddAccountRoute) },
                        )
                    }

                    entry<IosEditTransactionRoute> { key ->
                        EditTransaction(
                            transactionId = key.transactionId,
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            onAddNewAccount = { backStack.add(IosAddAccountRoute) },
                        )
                    }

                    entry<IosAddAccountRoute> {
                        AddAccountScreen(
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                        )
                    }

                    entry<IosCategoriesListRoute> {
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
                            onAddCategory = { backStack.add(IosAddCategoryRoute()) },
                            onBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    entry<IosAddCategoryRoute> { key ->
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

                    entry<IosRecurringRoute> {
                        IosRecurringEntry(
                            onNavigateToAddEdit = { id -> backStack.add(IosAddEditRecurringRoute(id)) },
                            onShowError = showRootMessage,
                        )
                    }

                    entry<IosAddEditRecurringRoute> { key ->
                        AddEditRecurringMovementScreen(
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            id = key.id,
                        )
                    }

                    entry<IosReportRoute> {
                        ReportScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onAddTransaction = { backStack.add(IosAddTransactionRoute) },
                            // Sharing is a platform concern — no-op for 5b.
                            onShareText = { /* TODO phase 6b: iOS share sheet */ },
                        )
                    }

                    entry<IosAuthRoute> {
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

/** Exit-through-home tab switch, mirroring Android's switchTab. */
private fun NavBackStack<NavKey>.switchTab(target: IosBottomBarRoute) {
    clear()
    add(START_TAB)
    if (target != START_TAB) add(target)
}

/** Pop intermediate routes until the (add/edit) transaction screen is on top, so it receives pendingCategory. */
private fun NavBackStack<NavKey>.popToTransactionScreen() {
    while (isNotEmpty() && last() !is IosAddTransactionRoute && last() !is IosEditTransactionRoute) {
        removeLastOrNull()
    }
}

// Spanish copy for ProfileViewModel notifications surfaced via the root snackbar (mirrors Android's
// ProfileMessage.toText). On iOS 6a only SessionClosed / AccountDeleted can fire (export/import is 6b);
// the rest are mapped for exhaustiveness so a future iOS path stays covered.
private fun ProfileMessage.toIosText(): String = when (this) {
    ProfileMessage.SessionClosed -> "Sesión cerrada. Tus datos siguen en este teléfono."
    ProfileMessage.AccountDeleted -> "Cuenta eliminada. Tus datos siguen en este teléfono."
    ProfileMessage.ExportDone -> "Listo, tu data está guardada."
    ProfileMessage.ExportFailed -> "No pude exportar — capaz no hay espacio en tu celu?"
    is ProfileMessage.ImportDone -> "Listo — $transactions movimientos importados."
    ProfileMessage.ImportFailed -> "No pude importar el archivo — capaz está dañado."
}

private data class IosBottomTab(
    val route: IosBottomBarRoute?, // null = add pseudo-tab
    val label: String,
    val icon: ImageVector,
    val isAdd: Boolean = false,
)

private val IOS_BOTTOM_TABS = listOf(
    IosBottomTab(IosHomeRoute, "Inicio", Icons.Outlined.Home),
    IosBottomTab(IosSeeTransactionsRoute, "Ver", Icons.AutoMirrored.Outlined.List),
    IosBottomTab(null, "Agregar", Icons.Outlined.Add, isAdd = true),
    IosBottomTab(IosAccountsRoute, "Cuentas", Icons.Outlined.AccountBalanceWallet),
    IosBottomTab(IosProfileRoute, "Perfil", Icons.Outlined.Person),
)

@Composable
private fun IosBottomBar(
    current: IosBottomBarRoute?,
    onTabClick: (IosBottomBarRoute) -> Unit,
    onAddClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(1.dp)
                .background(colors.border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IOS_BOTTOM_TABS.forEach { tab ->
                if (tab.isAdd) {
                    IosAddBottomBarItem(
                        label = tab.label,
                        onClick = onAddClick,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    IosRegularBottomBarItem(
                        label = tab.label,
                        icon = tab.icon,
                        isActive = tab.route == current,
                        onClick = { tab.route?.let(onTabClick) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun IosRegularBottomBarItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val tint = if (isActive) colors.textPrimary else colors.textDisabled
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            letterSpacing = 0.1.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun IosAddBottomBarItem(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.accent),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.size(3.dp))
        Text(
            text = label,
            color = colors.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            letterSpacing = 0.1.sp,
            maxLines = 1,
        )
    }
}
