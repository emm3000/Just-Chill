package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.FrequentCombo
import com.emm.domain.transaction.GetFrequentCombosUseCase
import com.emm.domain.transaction.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSpendShortcutCombosTest {

    private val bcp = Account(AccountId("bcp"), "BCP")
    private val yape = Account(AccountId("yape"), "Yape")
    private val food = Category(CategoryId("food"), "Comida", "icon", "color", CategoryType.Spend)
    private val transport = Category(CategoryId("transport"), "Transporte", "icon", "color", CategoryType.Spend)

    private val getFrequentCombos = mockk<GetFrequentCombosUseCase>()
    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(listOf(bcp, yape))
    }
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(listOf(food, transport))
    }

    private val getShortcutCombos = GetSpendShortcutCombos(getFrequentCombos, accountRepository, categoryRepository)

    @Test
    fun `asks GetFrequentCombosUseCase for Spend`() = runTest {
        coEvery { getFrequentCombos(any(), any(), any()) } returns emptyList()

        getShortcutCombos()

        coVerify { getFrequentCombos(TransactionType.Spend, limit = 5) }
    }

    @Test
    fun `fewer combos than the cap are returned as-is`() = runTest {
        coEvery { getFrequentCombos(any(), any(), any()) } returns
            listOf(FrequentCombo(bcp.accountId, food.categoryId, TransactionType.Spend))

        val result = getShortcutCombos()

        assertEquals(1, result.size)
    }

    @Test
    fun `a combo whose account no longer resolves is pruned, not crashed`() = runTest {
        coEvery { getFrequentCombos(any(), any(), any()) } returns listOf(
            FrequentCombo(AccountId("deleted"), food.categoryId, TransactionType.Spend),
            FrequentCombo(yape.accountId, transport.categoryId, TransactionType.Spend),
        )

        val result = getShortcutCombos()

        assertEquals(listOf(transport.categoryId.value), result.map { it.categoryId })
    }

    @Test
    fun `a combo whose category no longer resolves is pruned, not crashed`() = runTest {
        coEvery { getFrequentCombos(any(), any(), any()) } returns listOf(
            FrequentCombo(bcp.accountId, CategoryId("deleted"), TransactionType.Spend),
            FrequentCombo(yape.accountId, food.categoryId, TransactionType.Spend),
        )

        val result = getShortcutCombos()

        assertEquals(listOf(yape.accountId.value), result.map { it.accountId })
    }

    @Test
    fun `title is the category alone, subtitle names the account too`() = runTest {
        coEvery { getFrequentCombos(any(), any(), any()) } returns
            listOf(FrequentCombo(bcp.accountId, food.categoryId, TransactionType.Spend))

        val combo = getShortcutCombos().single()

        assertEquals("Comida", combo.title)
        assertEquals("BCP · Comida", combo.subtitle)
        assertEquals("Spend", combo.type)
    }

    @Test
    fun `resolving combos are capped at three, most-used first`() = runTest {
        val combos = listOf(
            FrequentCombo(bcp.accountId, food.categoryId, TransactionType.Spend),
            FrequentCombo(yape.accountId, food.categoryId, TransactionType.Spend),
            FrequentCombo(bcp.accountId, transport.categoryId, TransactionType.Spend),
            FrequentCombo(yape.accountId, transport.categoryId, TransactionType.Spend),
        )
        coEvery { getFrequentCombos(any(), any(), any()) } returns combos

        val result = getShortcutCombos()

        assertEquals(3, result.size)
        assertEquals(
            combos.take(3).map { it.categoryId.value },
            result.map { it.categoryId },
            "capping must keep the incoming (most-used-first) order, not reorder it",
        )
    }

    @Test
    // MockK verify{} records an expectation instead of consuming a result — IgnoredReturnValue
    // doesn't know that.
    @Suppress("IgnoredReturnValue")
    fun `no combos in the window returns an empty list without touching the repositories`() = runTest {
        coEvery { getFrequentCombos(any(), any(), any()) } returns emptyList()

        val result = getShortcutCombos()

        assertTrue(result.isEmpty())
        verify(exactly = 0) { accountRepository.all() }
    }
}
