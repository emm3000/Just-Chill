package com.emm.justchill.feature.transaction.capture

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import com.emm.justchill.feature.transaction.AddTransactionRoute
import com.emm.justchill.feature.transaction.EditTransactionRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.transactionEntries(
    bindings: NavHostBindings,
    pendingCategory: () -> SelectableCategory?,
    onPendingCategoryConsumed: () -> Unit,
    onAddNewAccount: (AppNavigator) -> Unit,
    onAddNewCategory: (AppNavigator, CategoryType) -> Unit,
) {
    entry<AddTransactionRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
        val vm: AddTransactionViewModel = koinViewModel()

        LaunchedEffect(key) {
            vm.onIntent(
                AddTransactionIntent.OnPreselectCombo(
                    accountId = key.preselectedAccountId,
                    categoryId = key.preselectedCategoryId,
                    type = key.preselectedType,
                ),
            )
        }

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
            onAddNewCategory = { categoryType -> onAddNewCategory(nav, categoryType) },
            onAddNewAccount = { onAddNewAccount(nav) },
        )
    }

    entry<EditTransactionRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
        val vm: EditTransactionViewModel = koinViewModel(parameters = { parametersOf(key.transactionId) })

        LaunchedEffect(pendingCategory()) {
            pendingCategory()?.let { selectableCategory ->
                vm.onIntent(EditTransactionIntent.OnNewValueFromOthers(selectableCategory))
                onPendingCategoryConsumed()
            }
        }

        EditTransaction(
            transactionId = key.transactionId,
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            onAddNewCategory = { categoryType -> onAddNewCategory(nav, categoryType) },
            onAddNewAccount = { onAddNewAccount(nav) },
            vm = vm,
        )
    }
}
