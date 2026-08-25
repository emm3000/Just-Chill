package com.emm.justchill.hh.transaction

import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
import com.emm.domain.transaction.GetFrequentCombosUseCase
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.comboLabel
import kotlinx.coroutines.flow.first

/**
 * Shortcut-ready combos for the launcher — ids, type and both labels as plain strings, so
 * `:androidApp` never needs a `:domain` enum to build an `Intent` (E09-03). Spend only: a shortcut
 * exists to log money that just left the wallet, and income is a planned monthly event nobody
 * reaches for a launcher shortcut to record.
 */
class GetSpendShortcutCombos(
    private val getFrequentCombos: GetFrequentCombosUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) {

    suspend operator fun invoke(): List<ShortcutCombo> {
        val combos = getFrequentCombos(TransactionType.Spend, limit = RESOLUTION_LIMIT)
        if (combos.isEmpty()) return emptyList()

        val accounts = accountRepository.all().first()
        val categories = categoryRepository.all().first()
        return combos.mapNotNull { combo ->
            val account = accounts.find { it.accountId == combo.accountId } ?: return@mapNotNull null
            val category = categories.find { it.categoryId == combo.categoryId } ?: return@mapNotNull null
            ShortcutCombo(
                accountId = combo.accountId.value,
                categoryId = combo.categoryId.value,
                type = combo.type.name,
                title = category.name,
                subtitle = comboLabel(account.name, category.name),
            )
        }.take(SHORTCUT_CAP)
    }

    private companion object {
        const val SHORTCUT_CAP = 3
        const val RESOLUTION_LIMIT = 5
    }
}
