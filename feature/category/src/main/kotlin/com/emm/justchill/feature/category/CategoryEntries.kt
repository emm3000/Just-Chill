package com.emm.justchill.feature.category

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.category.toSelectable
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.categoryEntries(
    bindings: NavHostBindings,
    onCategoryForTransaction: (SelectableCategory) -> Unit,
) {
    entry<CategoriesListRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
        val vm: CategoriesViewModel = koinViewModel()
        val categoriesState by vm.state.collectAsStateWithLifecycle()

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    is CategoriesEffect.ShowMessage -> bindings.showMessage(effect.text)
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

    entry<CategoryRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
        AddCategoryScreen(
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            onCategorySave = { created ->
                if (key.propagateToTransaction) {
                    onCategoryForTransaction(created.toSelectable())
                    nav.popToCapture()
                } else {
                    bindings.showMessage("Categoría «${created.name}» creada")
                    nav.pop()
                }
            },
            vm = koinViewModel(
                parameters = { parametersOf(key.initialType, key.initialName) },
            ),
        )
    }
}
