package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.CreateTransactionUseCase
import com.emm.justchill.core.domain.transaction.Transaction
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class AddTransactionMonthSpendTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val lima: TimeZone = TimeZone.of("America/Lima")
    private val today: LocalDate = LocalDate(2026, Month.AUGUST, 10)
    private val firstOfSeptember: LocalDate = LocalDate(2026, Month.SEPTEMBER, 1)
    private val august: YearMonth = YearMonth(2026, Month.AUGUST)
    private val september: YearMonth = YearMonth(2026, Month.SEPTEMBER)

    private val todayDates: MutableStateFlow<LocalDate> = MutableStateFlow(today)
    private val augustRows: MutableStateFlow<List<Transaction>> = MutableStateFlow(emptyList())
    private val septemberRows: MutableStateFlow<List<Transaction>> = MutableStateFlow(emptyList())

    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(
            LocalDateTime(today, LocalTime(14, 30)).toInstant(lima).toEpochMilliseconds(),
        )
    }

    private val account: Account = Account(AccountId("yape"), "Yape")
    private val category: Category = Category(
        categoryId = CategoryId("food"),
        name = "Comida",
        icon = "food",
        color = "blue",
        categoryType = CategoryType.Spend,
    )

    private val transactionRepository: TransactionRepository = mockk {
        every { allInRange(august.startInclusiveDay(), august.endExclusiveDay()) } returns augustRows
        every { allInRange(september.startInclusiveDay(), september.endExclusiveDay()) } returns septemberRows
    }

    private val createTransaction: CreateTransactionUseCase = mockk(relaxed = true)

    private fun spendOf(cents: Long): Transaction = Transaction(
        transactionId = TransactionId("tx-$cents"),
        type = TransactionType.Spend,
        amount = Money(cents),
        description = "",
        occurredAt = LocalDateTime(today, LocalTime(14, 30)),
        accountId = account.accountId,
        categoryId = category.categoryId,
    )

    private fun buildViewModel(): AddTransactionViewModel = addTransactionViewModel(
        todayDates = todayDates,
        clock = fixedClock,
        zone = lima,
        accountRepository = mockk { every { all() } returns flowOf(listOf(account)) },
        categoryRepository = mockk { every { all() } returns flowOf(listOf(category)) },
        transactionRepository = transactionRepository,
        createTransaction = createTransaction,
    )

    @Test
    fun `the month total re-emits once the saved movement lands`() = runTest(testDispatcher) {
        coEvery { createTransaction.invoke(any()) } answers { augustRows.value = listOf(spendOf(85_40L)) }

        val vm: AddTransactionViewModel = buildViewModel()
        val recorded: MutableList<MonthSpend> = mutableListOf()
        val job: Job = launch {
            vm.state.map { state -> state.monthSpend }.distinctUntilChanged().collect(recorded::add)
        }
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        advanceUntilIdle()
        vm.onIntent(AddTransactionIntent.OnSave)
        advanceUntilIdle()

        assertEquals(
            listOf(MonthSpend(august, Money.Zero), MonthSpend(august, Money(85_40L))),
            recorded,
        )
        job.cancel()
    }

    @Test
    fun `the month total follows the day over midnight into the next month`() = runTest(testDispatcher) {
        augustRows.value = listOf(spendOf(85_40L))
        septemberRows.value = listOf(spendOf(12_00L))

        val vm: AddTransactionViewModel = buildViewModel()
        val recorded: MutableList<MonthSpend> = mutableListOf()
        val job: Job = launch {
            vm.state.map { state -> state.monthSpend }.distinctUntilChanged().collect(recorded::add)
        }
        advanceUntilIdle()

        todayDates.value = firstOfSeptember
        advanceUntilIdle()

        assertEquals(
            listOf(
                MonthSpend(august, Money.Zero),
                MonthSpend(august, Money(85_40L)),
                MonthSpend(september, Money(12_00L)),
            ),
            recorded,
        )
        job.cancel()
    }

    @Test
    fun `the label names the month the total was read for`() = runTest(testDispatcher) {
        val vm: AddTransactionViewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals("Gastado en Agosto", vm.state.value.monthSpendLabel)
    }
}
