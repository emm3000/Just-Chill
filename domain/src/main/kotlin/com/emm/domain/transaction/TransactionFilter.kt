package com.emm.domain.transaction

import com.emm.domain.shared.CategoryId

data class TransactionFilter(val query: String = "", val categoryIds: Set<CategoryId> = emptySet()) {
    val isEmpty: Boolean
        get() = query.isBlank() && categoryIds.isEmpty()

    companion object {
        val None = TransactionFilter()
    }
}
