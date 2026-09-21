package com.emm.justchill.core.ui.transaction

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.ui.category.SelectableCategory

// Loading is not an empty catalog: only the distinction between them separates "there are no
// accounts yet" from "the rows have not arrived".
sealed interface Catalog {

    // Empty while Loading, which is why loaded and not this is what tells the two apart.
    val accounts: List<Account>

    // Non-null once the rows have arrived; each form cuts its own category list out of it.
    val loaded: Loaded?

    data object Loading : Catalog {

        override val accounts: List<Account> = emptyList()

        override val loaded: Loaded? = null
    }

    data class Loaded(
        override val accounts: List<Account>,
        val categories: Map<CategoryType, List<SelectableCategory>>,
    ) : Catalog {

        override val loaded: Loaded get() = this
    }
}

fun Catalog.categoriesOf(type: CategoryType, extras: List<SelectableCategory>): List<SelectableCategory> {
    val known: List<SelectableCategory> = loaded?.categories?.get(type).orEmpty()
    val pending: List<SelectableCategory> = extras.filter { extra ->
        extra.categoryType == type && known.none { it.categoryId == extra.categoryId }
    }
    return if (pending.isEmpty()) known else pending + known
}
