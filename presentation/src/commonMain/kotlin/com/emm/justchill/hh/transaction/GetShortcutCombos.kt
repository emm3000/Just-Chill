package com.emm.justchill.hh.transaction

import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
import com.emm.domain.transaction.GetFrequentCombosUseCase
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.flow.first

/**
 * Shortcut-ready combos for the launcher — ids, type and both labels as plain strings, so
 * `:androidApp` never needs a `:domain` enum to build an `Intent` (E09-03).
 */
class GetShortcutCombos(
    private val getFrequentCombos: GetFrequentCombosUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) {

    // Spend only: a shortcut exists to log money that just left the wallet, and income is a
    // planned monthly event nobody reaches for a launcher shortcut to record.
    suspend operator fun invoke(): List<ShortcutCombo> {
        val combos = getFrequentCombos(TransactionType.Spend)
        if (combos.isEmpty()) return emptyList()

        val categories = categoryRepository.all().first()
        // Requests GetFrequentCombosUseCase's own limit (5), above SHORTCUT_CAP, so a combo whose
        // account or category was deleted can be dropped and still leave three that resolve.
        return combos.mapNotNull { combo ->
            val account = accountRepository.find(combo.accountId) ?: return@mapNotNull null
            val category = categories.find { it.categoryId == combo.categoryId } ?: return@mapNotNull null
            ShortcutCombo(
                accountId = combo.accountId.value,
                categoryId = combo.categoryId.value,
                type = combo.type.name,
                shortLabel = category.name,
                longLabel = "${account.name} · ${category.name}",
            )
        }.take(SHORTCUT_CAP)
    }

    private companion object {
        const val SHORTCUT_CAP = 3
    }
}
