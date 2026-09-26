package com.emm.justchill.feature.transaction.capture

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.account.AccountRepository
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.transaction.CreateTransactionUseCase
import com.emm.justchill.core.domain.transaction.GetFrequentCombosUseCase
import com.emm.justchill.core.domain.transaction.GetMonthSpendUseCase
import com.emm.justchill.core.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.domain.transaction.TransactionStatsRepository
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.testing.FakeTodayFlow
import com.emm.justchill.core.ui.theme.EmmTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class PadExitTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private val lima: TimeZone = TimeZone.of("America/Lima")
    private val today: LocalDate = LocalDate(2026, Month.AUGUST, 10)
    private val account: Account = Account(AccountId("yape"), "Yape")

    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(
            LocalDateTime(today, LocalTime(14, 30)).toInstant(lima).toEpochMilliseconds(),
        )
    }

    private fun padViewModel(): AddTransactionViewModel {
        val getTopUsedCategoryIds: GetTopUsedCategoryIdsUseCase = mockk()
        coEvery { getTopUsedCategoryIds.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns emptyList()
        val getFrequentCombos: GetFrequentCombosUseCase = mockk()
        coEvery { getFrequentCombos.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns emptyList()
        coEvery {
            getFrequentCombos.invoke(any<TransactionType>(), any<Int>(), any<Int>(), any<Money>())
        } returns emptyList()
        val transactionStatsRepository: TransactionStatsRepository = mockk()
        coEvery { transactionStatsRepository.lastUsedAccountId() } returns null
        val transactionRepository: TransactionRepository = mockk {
            every { allInRange(any(), any()) } returns flowOf(emptyList())
        }
        val accountRepository: AccountRepository = mockk {
            every { all() } returns flowOf(listOf(account))
        }
        val categoryRepository: CategoryRepository = mockk {
            every { all() } returns flowOf(emptyList())
        }
        return AddTransactionViewModel(
            createTransaction = mockk<CreateTransactionUseCase>(relaxed = true),
            getTopUsedCategoryIds = getTopUsedCategoryIds,
            getFrequentCombos = getFrequentCombos,
            getMonthSpend = GetMonthSpendUseCase(transactionRepository),
            transactionStatsRepository = transactionStatsRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            todayFlow = FakeTodayFlow(MutableStateFlow(today)),
            clock = fixedClock,
            zone = lima,
        )
    }

    @Test
    fun `the close key leaves the pad once`() {
        var closed: Int = 0

        composeRule.setContent {
            EmmTheme {
                AddTransactionScreenContent(
                    state = AddTransactionUiState(today = today),
                    onIntent = {},
                    onClose = { closed += 1 },
                    onOpenTransactions = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Cerrar").performClick()

        assertEquals(1, closed)
    }

    @Test
    fun `a saved movement leaves through the save exit, never through the close key`() {
        val vm: AddTransactionViewModel = padViewModel()
        val exits: MutableList<String> = mutableListOf()

        composeRule.setContent {
            EmmTheme {
                AddTransactionScreen(
                    vm = vm,
                    onClose = { exits += "close" },
                    onSaveSuccess = { exits += "saved" },
                    snackbarHostState = SnackbarHostState(),
                    onOpenTransactions = {},
                )
            }
        }
        composeRule.waitUntil { vm.state.value.accountSelected == account }

        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        vm.onIntent(AddTransactionIntent.OnSave)
        composeRule.waitUntil { exits.isNotEmpty() }
        composeRule.waitForIdle()

        assertEquals(listOf("saved"), exits)
    }
}
