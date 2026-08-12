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

    // Stated, not inherited. HomeViewModel takes no defaults, so the suite says when and where it
    // is: noon UTC mid-month, which is May 2026 in every zone and matches [period] below. The
    // helper defaults to that pair so the two tests that vary the zone are the only ones spelling
    // one out — and so nothing here can quietly go back to reading the machine.
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
        // One instant, two zones, two different months: 2026-09-01T02:00Z is already September at
        // UTC and still 31 August at UTC-5. Which month Home opens on is therefore a question about
        // the zone, and the zone that answers it has to be the injected one — the same one this
        // ViewModel already uses for its Hoy/Ayer labels. Otherwise half the screen answers for the
        // device and half for the injection.
        //
        // Both zones are asserted because one proves nothing: on a machine whose own clock sits in
        // that zone the ambient read agrees, and the test stays green straight through the bug.
        // The dev machine here is America/Lima, which is exactly UTC-5.
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

    // ---- Scenario 10.1: No pending → section absent ----

    @Test
    fun `10_1 empty pending list maps to empty pendingRecurringMovements in state`() = runTest {
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        advanceUntilIdle()

        assertTrue(viewModel.state.value.pendingRecurringMovements.isEmpty())
    }

    // ---- Scenario 10.2: One or more pending → section visible ----

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
        // isCatchUp is `period < YearMonth.current(clock, zone)`, so it is a question about the
        // injected clock and nothing else. Worth pinning explicitly, because fixing the clock at
        // May 2026 — the same month as `period` — made every other pending fixture in this suite
        // land on `false`, and the catch-up branch became unreachable from here. Under the ambient
        // clock it was silently `true` on any run after May 2026: the same assertion nobody wrote,
        // answered differently depending on the day. Both sides are asserted so neither can rot.
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

    // ---- Scenario 4.1: Confirm fixed amount successfully ----

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

    // ---- Scenario 4.2: Confirm fails with DatabaseError ----

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
        // Must carry the toUserMessage() string — never the raw exception message.
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

    // ---- Scenario 5.1: Variable amount — confirm button disabled while amount == 0 ----
    // This scenario is enforced by the Sheet composable (amount field required before enabling the
    // button). The ViewModel receives a valid callerAmount when the user taps Confirm, so there is
    // no ViewModel-level test for "button disabled"; instead we verify that callerAmount is
    // forwarded correctly and that null callerAmount is handled.

    @Test
    fun `5_1 ConfirmRecurring with variable amount forwards callerAmount null to use case`() = runTest {
        coEvery { confirmRecurring(any(), any(), null) } returns Unit
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-2", period, null))
        advanceUntilIdle()

        coVerify(exactly = 1) { confirmRecurring(any(), any(), null) }
    }

    // ---- Scenario 5.3: Variable amount zero rejected by use case ----

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

    // ---- Scenario 6.1: Double confirm same period rejected ----

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

    // ---- Catch-up: the period travels with the intent ----

    @Test
    fun `ConfirmRecurring forwards the intent's period, not the month on screen`() = runTest {
        coEvery { confirmRecurring(any(), any(), any()) } returns Unit
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = homeViewModel()

        val backdated = YearMonth(2026, Month.MARCH)
        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", backdated, Money(1800L)))
        advanceUntilIdle()

        // Reading the period off the selected month is what lost the missed month in the first place.
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
