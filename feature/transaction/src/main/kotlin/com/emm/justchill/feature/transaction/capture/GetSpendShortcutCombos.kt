package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.AccountRepository
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.transaction.GetFrequentCombosUseCase
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.feature.transaction.capture.comboLabel
import kotlinx.coroutines.flow.first

// Plain strings, not domain enums, so :androidApp never needs a :core:domain type to build an Intent
// (E09-03). Spend only: income is a planned monthly event nobody reaches for a shortcut to record.
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
