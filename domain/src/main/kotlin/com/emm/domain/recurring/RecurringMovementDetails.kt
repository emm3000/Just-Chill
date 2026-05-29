package com.emm.domain.recurring

import com.emm.domain.shared.Money

/**
 * Read-only projection of a recurring movement template enriched with joined category
 * and account display fields. Used exclusively by the list screen read path.
 *
 * Deliberately omits write-only fields (description, categoryId, accountId, frequency,
 * lastConfirmedPeriod) to keep the read model minimal and the mapper obvious.
 */
data class RecurringMovementDetails(
    val id: String,
    val name: String,
    val type: com.emm.domain.transaction.TransactionType,
    /** null = variable-amount template */
    val amount: Money?,
    /** null when category was deleted (ON DELETE SET NULL) or never assigned */
    val categoryName: String?,
    /** Token key (e.g. "green", "blue"); null under same conditions as categoryName */
    val categoryColor: String?,
    /** Always present in practice (accountId FK is NOT NULL + ON DELETE RESTRICT) */
    val accountName: String?,
    val dayOfMonth: Int,
    val isActive: Boolean,
)
