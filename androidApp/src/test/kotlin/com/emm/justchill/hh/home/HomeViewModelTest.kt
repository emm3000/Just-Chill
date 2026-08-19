package com.emm.justchill.hh.home

import com.emm.domain.home.GetHomeDataUseCase
import com.emm.domain.home.HomeData
import com.emm.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.PendingRecurring
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.SkipRecurringMovementUseCase
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val getHomeData = mockk<GetHomeDataUseCase>()
    private val confirmRecurring = mockk<ConfirmRecurringMovementUseCase>()
    private val skipRecurring = mockk<SkipRecurringMovementUseCase>()

    private lateinit var viewModel: HomeViewModel

    /** Noon UTC mid-month is May 2026 in every zone. */
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-05-16T12:00:00Z")
    }
    private val utc = UtcOffset(hours = 0).asTimeZone()

    private fun homeViewModel(clock: Clock = fixedClock, zone: TimeZone = utc) =
        HomeViewModel(getHomeData, confirmRecurring, skipRecurring, clock, zone)

    private val emptyHomeData = HomeData(
        lastTransactions = emptyList(),
        income = Money.Zero,
        spend = Money.Zero,
        balance = Money.Zero,
        hasAnyTransaction = false,
        pendingRecurringMovements = emptyList(),
    )

    private val period = YearMonth(2026, Month.MAY)

    private fun pending(
        id: String = "rm-1",
        name: String = "Netflix",
        amount: Money? = Money(1800L),
        period: YearMonth = this.period,
    ) = PendingRecurring(recurringMovement(id = id, name = name, amount = amount), period)

    private fun recurringMovement(
        id: String = "rm-1",
        name: String = "Netflix",
        type: TransactionType = TransactionType.Spend,
        amount: Money? = Money(1800L),
        dayOfMonth: Int = 15,
    ) = RecurringMovement(
        id = RecurringMovementId(id),
        name = name,
        type = type,
        amount = amount,
        description = "",
        categoryId = null,
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = dayOfMonth,
        isActive = true,
        lastConfirmedPeriod = null,
        createdAt = 0L,
    )

    @Before
    fun setUp() {
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()
    }

    // The two `verify { }` calls below record an expectation instead of consuming a result, so
    // IgnoredReturnValue fires on them and means nothing. Suppressed on this function rather than
    // on the class: outside a verification block, "called it and dropped the result" is a real bug
    // in a test, and the rule should keep catching it everywhere else in this file.
    @Suppress("IgnoredReturnValue")
    @Test
    fun `the month it loads is read in the injected zone, not the device's`() = runTest {
        val nearMidnight = object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-01T02:00:00Z")
        }

        homeViewModel(nearMidnight, UtcOffset(hours = -5).asTimeZone())
        advanceUntilIdle()
        verify { getHomeData(YearMonth(2026, Month.AUGUST)) }

        homeViewModel(nearMidnight, UtcOffset(hours = 0).asTimeZone())
        advanceUntilIdle()
        verify { getHomeData(YearMonth(2026, Month.SEPTEMBER)) }
    }

    @Test
    fun `10_1 empty pending list maps to empty pendingRecurringMovements in state`() = runTest {
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        advanceUntilIdle()

        assertTrue(viewModel.state.value.pendingRecurringMovements.isEmpty())
    }

    @Test
    fun `10_2 two pending items map to two PendingRecurringUi entries`() = runTest {
        val homeData = emptyHomeData.copy(
            pendingRecurringMovements = listOf(
                pending("rm-1", "Netflix"),
                pending("rm-2", "Spotify", amount = null),
            ),
        )
        every { getHomeData(any()) } returns flowOf(homeData)
        viewModel = homeViewModel()

        advanceUntilIdle()

        val pending = viewModel.state.value.pendingRecurringMovements
        assertEquals(2, pending.size)
        assertEquals("rm-1", pending[0].templateId)
        assertEquals("rm-2", pending[1].templateId)
        assertFalse(pending[0].isVariableAmount)
        assertTrue(pending[1].isVariableAmount)
    }

    @Test
    fun `a period older than the clock's month is marked catch-up, the clock's own month is not`() = runTest {
        val homeData = emptyHomeData.copy(
            pendingRecurringMovements = listOf(
                pending("rm-old", "Netflix", period = YearMonth(2026, Month.MARCH)),
                pending("rm-now", "Spotify", period = period),
            ),
        )
        every { getHomeData(any()) } returns flowOf(homeData)
        viewModel = homeViewModel()

        advanceUntilIdle()

        val pending = viewModel.state.value.pendingRecurringMovements
        assertTrue(pending.single { it.templateId == "rm-old" }.isCatchUp, "March 2026 is a caught-up month")
        assertFalse(pending.single { it.templateId == "rm-now" }.isCatchUp, "May 2026 is the clock's own month")
    }

    @Test
    fun `4_1 ConfirmRecurring intent with fixed amount calls use case and emits CloseSheet effect`() = runTest {
        coEvery { confirmRecurring(any(), any(), any()) } returns Unit
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", period, Money(1800L)))
        advanceUntilIdle()

        coVerify(exactly = 1) { confirmRecurring(any(), any(), eq(Money(1800L))) }
        assertTrue(effects.any { it is HomeEffect.CloseConfirmSheet })
        job.cancel()
    }

    @Test
    fun `4_2 ConfirmRecurring propagates DomainException as ShowSnackbar effect`() = runTest {
        val error = DomainException.DatabaseError(RuntimeException("DB fail"))
        coEvery { confirmRecurring(any(), any(), any()) } throws error
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", period, Money(1800L)))
        advanceUntilIdle()

        assertTrue(effects.any { it is HomeEffect.ShowError })
        job.cancel()
    }

    @Test
    fun `4_2b ConfirmRecurring DatabaseError ShowError carries toUserMessage string not raw exception`() = runTest {
        val error = DomainException.DatabaseError(RuntimeException("raw internal message"))
        coEvery { confirmRecurring(any(), any(), any()) } throws error
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", period, Money(1800L)))
        advanceUntilIdle()

        val showError = effects.filterIsInstance<HomeEffect.ShowError>().firstOrNull()
        checkNotNull(showError) { "Expected ShowError effect but got: $effects" }
        assertEquals("Hubo un problema guardando tu data", showError.message)
        assertFalse(showError.message.contains("raw internal message"))
        job.cancel()
    }

    @Test
    fun `4_1b ConfirmRecurring success emits CloseConfirmSheet — not ShowError`() = runTest {
        coEvery { confirmRecurring(any(), any(), any()) } returns Unit
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", period, Money(1800L)))
        advanceUntilIdle()

        assertTrue(effects.any { it is HomeEffect.CloseConfirmSheet }, "Expected CloseConfirmSheet but got: $effects")
        assertFalse(effects.any { it is HomeEffect.ShowError }, "Should not emit ShowError on success")
        job.cancel()
    }

    @Test
    fun `5_1 ConfirmRecurring with variable amount forwards callerAmount null to use case`() = runTest {
        coEvery { confirmRecurring(any(), any(), null) } returns Unit
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-2", period, null))
        advanceUntilIdle()

        coVerify(exactly = 1) { confirmRecurring(any(), any(), null) }
    }

    @Test
    fun `5_3 ConfirmRecurring with zero callerAmount propagates ValidationError as ShowError effect`() = runTest {
        val error = DomainException.ValidationError("Amount must be > 0")
        coEvery { confirmRecurring(any(), any(), Money(0L)) } throws error
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-2", period, Money(0L)))
        advanceUntilIdle()

        assertTrue(effects.any { it is HomeEffect.ShowError })
        job.cancel()
    }

    @Test
    fun `6_1 ConfirmRecurring already-confirmed period propagates ValidationError as ShowError`() = runTest {
        val error = DomainException.ValidationError("Already confirmed for this period")
        coEvery { confirmRecurring(any(), any(), any()) } throws error
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", period, Money(1800L)))
        advanceUntilIdle()

        assertTrue(effects.any { it is HomeEffect.ShowError })
        job.cancel()
    }

    @Test
    fun `ConfirmRecurring forwards the intent's period, not the month on screen`() = runTest {
        coEvery { confirmRecurring(any(), any(), any()) } returns Unit
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val backdated = YearMonth(2026, Month.MARCH)
        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", backdated, Money(1800L)))
        advanceUntilIdle()

        coVerify(exactly = 1) { confirmRecurring(RecurringMovementId("rm-1"), backdated, Money(1800L)) }
    }

    @Test
    fun `SkipRecurring settles the period and closes the sheet`() = runTest {
        coEvery { skipRecurring(any(), any()) } returns Unit
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.SkipRecurring("rm-1", period))
        advanceUntilIdle()

        coVerify(exactly = 1) { skipRecurring(RecurringMovementId("rm-1"), period) }
        coVerify(exactly = 0) { confirmRecurring(any(), any(), any()) }
        assertTrue(effects.any { it is HomeEffect.CloseConfirmSheet })
        job.cancel()
    }

    @Test
    fun `SkipRecurring surfaces a domain failure as ShowError`() = runTest {
        coEvery { skipRecurring(any(), any()) } throws
            DomainException.DatabaseError(RuntimeException("DB fail"))
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.SkipRecurring("rm-1", period))
        advanceUntilIdle()

        assertTrue(effects.any { it is HomeEffect.ShowError })
        job.cancel()
    }
}
