package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.CategoryId

data class TransactionFilter(val query: String = "", val categoryIds: Set<CategoryId> = emptySet()) {
    val isEmpty: Boolean
        get() = query.isBlank() && categoryIds.isEmpty()

    companion object {
        val None = TransactionFilter()
    }
}
