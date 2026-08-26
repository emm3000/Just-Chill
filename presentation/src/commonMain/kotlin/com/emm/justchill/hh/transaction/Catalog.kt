package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.category.CategoryType

/**
 * What the three movement forms may offer. [Loading] is not an empty catalog: only the distinction
 * between them separates "there are no accounts yet" from "the rows have not arrived", and every
 * selection in a form is an id this resolves rather than an object a reducer cached.
 */
sealed interface Catalog {

    data object Loading : Catalog

    data class Loaded(val accounts: List<Account>, val categories: Map<CategoryType, List<SelectableCategory>>) :
        Catalog
}
