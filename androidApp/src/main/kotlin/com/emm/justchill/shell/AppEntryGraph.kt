package com.emm.justchill.shell

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.feature.account.AccountsRoute
import com.emm.justchill.feature.account.AddAccountRoute
import com.emm.justchill.feature.account.accountEntries
import com.emm.justchill.feature.auth.AuthRoute
import com.emm.justchill.feature.auth.authEntries
import com.emm.justchill.feature.category.CategoriesListRoute
import com.emm.justchill.feature.category.CategoryRoute
import com.emm.justchill.feature.category.categoryEntries
import com.emm.justchill.feature.loan.LoansRoute
import com.emm.justchill.feature.loan.loanEntries
import com.emm.justchill.feature.onboarding.ManifestoRoute
import com.emm.justchill.feature.onboarding.onboardingEntries
import com.emm.justchill.feature.profile.ProfileRoute
import com.emm.justchill.feature.profile.profileEntries
import com.emm.justchill.feature.recurring.RecurringMovementsRoute
import com.emm.justchill.feature.recurring.recurringEntries
import com.emm.justchill.feature.report.ReportRoute
import com.emm.justchill.feature.report.reportEntries
import com.emm.justchill.feature.transaction.AddTransactionRoute
import com.emm.justchill.feature.transaction.SeeTransactionRoute
import com.emm.justchill.feature.transaction.capture.transactionEntries
import com.emm.justchill.feature.transaction.list.seeTransactionsEntries

internal val HOME_ROUTE: AddTransactionRoute = AddTransactionRoute()

@Stable
internal class HostResultChannels(
    val pendingCategory: () -> SelectableCategory?,
    val onCategoryCaptured: (SelectableCategory) -> Unit,
    val onPendingCategoryConsumed: () -> Unit,
    val pendingImportJson: () -> String?,
    val onImportHandled: () -> Unit,
)

internal fun EntryProviderScope<NavKey>.appEntryGraph(
    bindings: NavHostBindings,
    channels: HostResultChannels,
    appVersion: String,
    commitHash: String,
    onFirstLaunchSeen: () -> Unit,
) {
    onboardingEntries(bindings, home = HOME_ROUTE, onFirstLaunchSeen = onFirstLaunchSeen)
    authEntries(bindings)
    seeTransactionsEntries(bindings)
    accountEntries(
        bindings = bindings,
        onOpenLoans = { nav -> nav.pushToTop(LoansRoute) },
    )
    loanEntries(bindings)
    categoryEntries(
        bindings = bindings,
        onCategoryForTransaction = channels.onCategoryCaptured,
    )
    transactionEntries(
        bindings = bindings,
        pendingCategory = channels.pendingCategory,
        onPendingCategoryConsumed = channels.onPendingCategoryConsumed,
        onAddNewAccount = { nav -> nav.push(AddAccountRoute) },
        onAddNewCategory = { nav, categoryType ->
            nav.push(CategoryRoute(initialType = categoryType, propagateToTransaction = true))
        },
        onOpenMenu = { nav -> nav.push(ProfileRoute) },
    )
    reportEntries(
        bindings = bindings,
        onAddTransaction = { nav -> nav.pushToTop(HOME_ROUTE) },
    )
    recurringEntries(
        bindings = bindings,
        pendingCategory = channels.pendingCategory,
        onPendingCategoryConsumed = channels.onPendingCategoryConsumed,
        onAddNewCategory = { nav, categoryType ->
            nav.push(CategoryRoute(initialType = categoryType, propagateToTransaction = true))
        },
    )
    profileEntries(
        bindings = bindings,
        appVersion = appVersion,
        commitHash = commitHash,
        pendingImportJson = channels.pendingImportJson,
        onImportHandled = channels.onImportHandled,
        onTransactionsClick = { nav -> nav.pushToTop(SeeTransactionRoute) },
        onReportClick = { nav -> nav.pushToTop(ReportRoute) },
        onAccountsClick = { nav -> nav.pushToTop(AccountsRoute) },
        onCategoriesClick = { nav -> nav.pushToTop(CategoriesListRoute) },
        onRecurringClick = { nav -> nav.pushToTop(RecurringMovementsRoute) },
        onLoansClick = { nav -> nav.pushToTop(LoansRoute) },
        onAboutClick = { nav -> nav.push(ManifestoRoute(isRevisit = true)) },
        onSignInClick = { nav -> nav.push(AuthRoute) },
    )
}
