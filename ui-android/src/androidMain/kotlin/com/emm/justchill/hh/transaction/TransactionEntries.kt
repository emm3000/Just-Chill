package com.emm.justchill.hh.transaction

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.AddAccountRoute
import com.emm.justchill.hh.shared.AddTransactionRoute
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.CategoryRoute
import com.emm.justchill.hh.shared.EditTransactionRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel

/**
 * Registers the transaction entries on the host: [AddTransactionRoute] and [EditTransactionRoute].
 *
 * @param pendingCategory reads the host's "category just created for this form" channel. It is a
 *   lambda, not a value, on purpose: `rememberDecoratedNavEntries` caches the built entries under
 *   `remember(backStack.toList())`, so an entry that closed over a plain value would keep whatever
 *   the host held at the last back-stack change. Reading through the lambda keeps the snapshot read
 *   inside the entry's own composition scope, exactly where it happened before this split.
 * @param onPendingCategoryConsumed clears that channel once the form has taken the category.
 */
fun EntryProviderScope<NavKey>.transactionEntries(
    bindings: NavHostBindings,
    pendingCategory: () -> SelectableCategory?,
    onPendingCategoryConsumed: () -> Unit,
) {
    entry<AddTransactionRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: AddTransactionViewModel = koinViewModel()

        LaunchedEffect(pendingCategory()) {
            pendingCategory()?.let { selectableCategory ->
                vm.onIntent(AddTransactionIntent.OnNewValueFromOthers(selectableCategory))
                onPendingCategoryConsumed()
            }
        }

        AddTransactionScreen(
            vm = vm,
            popBackStack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            // The movement's own type travels with the push. Without it the new-category screen
            // fell back to CategoryRoute's Spend default, so a category created from an Income
            // movement was born a Spend one — which the schema now refuses outright, and which is
            // how an Income movement ended up carrying a Spend category in the first place. The
            // default itself stays: the other two call sites open the screen with no movement
            // asking, and Spend is right there.
            onAddNewCategory = { categoryType ->
                nav.push(
                    CategoryRoute(
                        initialType = categoryType,
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
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        EditTransaction(
            transactionId = key.transactionId,
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            onAddNewAccount = { nav.push(AddAccountRoute) },
        )
    }
}
