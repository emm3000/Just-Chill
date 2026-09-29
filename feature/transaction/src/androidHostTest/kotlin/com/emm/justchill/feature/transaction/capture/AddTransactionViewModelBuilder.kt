package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.AccountRepository
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.transaction.CreateTransactionUseCase
import com.emm.justchill.core.domain.transaction.GetFrequentCombosUseCase
import com.emm.justchill.core.domain.transaction.GetMonthSpendUseCase
import com.emm.justchill.core.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.domain.transaction.TransactionStatsRepository
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.testing.FakeTodayFlow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

@Suppress("LongParameterList")
internal fun addTransactionViewModel(
    todayDates: MutableStateFlow<LocalDate>,
    clock: Clock,
    zone: TimeZone,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository = transactionRepositoryWithNoRows(),
    createTransaction: CreateTransactionUseCase = mockk(relaxed = true),
    getTopUsedCategoryIds: GetTopUsedCategoryIdsUseCase = getTopUsedCategoryIdsWithNoHistory(),
    getFrequentCombos: GetFrequentCombosUseCase = getFrequentCombosWithNoHistory(),
    transactionStatsRepository: TransactionStatsRepository = transactionStatsWithNoLastUsedAccount(),
): AddTransactionViewModel = AddTransactionViewModel(
    createTransaction = createTransaction,
    getTopUsedCategoryIds = getTopUsedCategoryIds,
    getFrequentCombos = getFrequentCombos,
    getMonthSpend = GetMonthSpendUseCase(transactionRepository),
    transactionStatsRepository = transactionStatsRepository,
    accountRepository = accountRepository,
    categoryRepository = categoryRepository,
    todayFlow = FakeTodayFlow(todayDates),
    clock = clock,
    zone = zone,
)

internal fun getTopUsedCategoryIdsWithNoHistory(): GetTopUsedCategoryIdsUseCase = mockk {
    coEvery { this@mockk.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns emptyList()
}

internal fun getFrequentCombosWithNoHistory(): GetFrequentCombosUseCase = mockk {
    coEvery { this@mockk.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns emptyList()
    coEvery { this@mockk.invoke(any<TransactionType>(), any<Int>(), any<Int>(), any<Money>()) } returns emptyList()
}

internal fun transactionStatsWithNoLastUsedAccount(): TransactionStatsRepository = mockk {
    coEvery { lastUsedAccountId() } returns null
}

private fun transactionRepositoryWithNoRows(): TransactionRepository = mockk {
    every { allInRange(any(), any()) } returns flowOf(emptyList())
}
