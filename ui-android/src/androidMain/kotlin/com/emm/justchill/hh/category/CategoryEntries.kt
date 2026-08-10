package com.emm.justchill.hh.category

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.domain.category.Category
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.CategoriesListRoute
import com.emm.justchill.hh.shared.CategoryRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.rememberAppNavigator
import com.emm.justchill.hh.transaction.SelectableCategory
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Registers the category entries on the host: [CategoriesListRoute] and [CategoryRoute].
 *
 * @param onCategoryForTransaction hands the freshly created category back to the transaction form
 *   that asked for it ([CategoryRoute.propagateToTransaction]). The host owns that result channel;
 *   this function only decides when it fires.
 */
fun EntryProviderScope<NavKey>.categoryEntries(
    bindings: NavHostBindings,
    onCategoryForTransaction: (SelectableCategory) -> Unit,
) {
    entry<CategoriesListRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
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
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        AddCategoryScreen(
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            onCategorySave = { created ->
                if (key.propagateToTransaction) {
                    onCategoryForTransaction(created.toSelectableCategory())
                    nav.popToTransaction()
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

/**
 * Resolves a just-saved domain [Category] into the [SelectableCategory] the transaction form renders.
 *
 * The domain stores the icon and the colour as opaque ids; the catalogs that turn them into drawables
 * and colours are UI-side, so the mapping belongs next to them rather than inlined in the nav host.
 */
private fun Category.toSelectableCategory(): SelectableCategory = SelectableCategory(
    categoryId = categoryId,
    name = name,
    iconId = icon,
    colorId = color,
    categoryType = categoryType,
)
