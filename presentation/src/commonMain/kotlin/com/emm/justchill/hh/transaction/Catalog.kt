package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.category.CategoryType

/**
 * What the three movement forms may offer. [Loading] is not an empty catalog: only the distinction
 * between them separates "there are no accounts yet" from "the rows have not arrived", and every
 * selection in a form is an id this resolves rather than an object a reducer cached.
 */
sealed interface Catalog {

    /** Empty while [Loading], which is why [loaded] and not this is what tells the two apart. */
    val accounts: List<Account>

    /** Non-null once the rows have arrived; each form cuts its own category list out of it. */
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
