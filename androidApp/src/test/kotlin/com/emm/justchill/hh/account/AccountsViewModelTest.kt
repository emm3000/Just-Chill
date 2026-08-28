package com.emm.justchill.hh.account

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountType
import com.emm.domain.account.DeleteAccountUseCase
import com.emm.domain.account.UpdateAccountUseCase
import com.emm.domain.loan.LoanRepository
import com.emm.domain.loan.PersonBalance
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.time.FakeTodayFlow
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.atTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AccountsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val accountRepository = mockk<AccountRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val loanRepository = mockk<LoanRepository>()
    private val updateAccount = mockk<UpdateAccountUseCase>()
    private val deleteAccount = mockk<DeleteAccountUseCase>()
    private val today = MutableStateFlow(LocalDate(2026, 8, 15))

    private lateinit var viewModel: AccountsViewModel

    private fun accountsViewModel() = AccountsViewModel(
        accountRepository,
        transactionRepository,
        loanRepository,
        updateAccount,
        deleteAccount,
        FakeTodayFlow(today),
    )

    private fun personBalance(
        personKey: String = "juan",
        personName: String = "Juan",
        remaining: Money = Money(10_000L),
    ) = PersonBalance(personKey = personKey, personName = personName, remaining = remaining)

    private val bcp = Account(accountId = AccountId("acc-1"), name = "BCP", type = AccountType.Bank)
    private val cash = Account(accountId = AccountId("acc-2"), name = "Efectivo", type = AccountType.Cash)

    private fun movement(
        id: String,
        account: Account,
        type: TransactionType,
        cents: Long,
        day: LocalDate = LocalDate(2026, 8, 10),
    ) = Transaction(
        transactionId = TransactionId(id),
        type = type,
        amount = Money(cents),
        description = "",
        occurredAt = day.atTime(hour = 12, minute = 0),
        accountId = account.accountId,
        categoryId = null,
    )

    private fun rowFor(account: Account) = viewModel.state.value.accounts.first { it.account == account }

    @Before
    fun setUp() {
        every { accountRepository.all() } returns flowOf(emptyList())
        every { transactionRepository.all() } returns flowOf(emptyList())
        every { loanRepository.balancesByPerson() } returns flowOf(emptyList())
        viewModel = accountsViewModel()
    }

    @Test
    fun `each account nets only its own movements, only from the current month`() = runTest {
        every { accountRepository.all() } returns flowOf(listOf(bcp, cash))
        every { transactionRepository.all() } returns flowOf(
            listOf(
                movement("t-1", bcp, TransactionType.Spend, 19_345L),
                movement("t-2", cash, TransactionType.Spend, 10_000L, day = LocalDate(2026, 7, 31)),
                movement("t-3", cash, TransactionType.Income, 90_000L, day = LocalDate(2026, 9, 1)),
            ),
        )
        viewModel = accountsViewModel()

        advanceUntilIdle()

        assertEquals("−S/ 193.45", rowFor(bcp).net)
        assertEquals(1, rowFor(bcp).movementCount)
        assertEquals("S/ 0.00", rowFor(cash).net)
        assertEquals(0, rowFor(cash).movementCount)
    }

    @Test
    fun `an account whose income outweighs its spend nets positive, unsigned`() = runTest {
        every { accountRepository.all() } returns flowOf(listOf(bcp, cash))
        every { transactionRepository.all() } returns flowOf(
            listOf(
                movement("t-1", bcp, TransactionType.Income, 350_000L),
                movement("t-2", bcp, TransactionType.Spend, 50_000L),
                movement("t-3", cash, TransactionType.Spend, 2_550L),
            ),
        )
        viewModel = accountsViewModel()

        advanceUntilIdle()

        assertEquals("S/ 3,000.00", rowFor(bcp).net)
        assertEquals(2, rowFor(bcp).movementCount)
        assertEquals("−S/ 25.50", rowFor(cash).net)
    }

    @Test
    fun `the header totals the whole month, both directions, across every account`() = runTest {
        every { accountRepository.all() } returns flowOf(listOf(bcp, cash))
        every { transactionRepository.all() } returns flowOf(
            listOf(
                movement("t-1", bcp, TransactionType.Spend, 19_345L),
                movement("t-2", cash, TransactionType.Spend, 2_550L),
                movement("t-3", bcp, TransactionType.Income, 350_000L),
                movement("t-4", cash, TransactionType.Income, 100_000L, day = LocalDate(2026, 7, 2)),
            ),
        )
        viewModel = accountsViewModel()

        advanceUntilIdle()

        assertEquals("S/ 218.95", viewModel.state.value.monthSpent)
        assertEquals("S/ 3,500.00", viewModel.state.value.monthIncome)
    }

    @Test
    fun `the month follows the clock past midnight, re-attributing what the header counts`() = runTest {
        every { accountRepository.all() } returns flowOf(listOf(bcp))
        every { transactionRepository.all() } returns flowOf(
            listOf(movement("t-1", bcp, TransactionType.Spend, 19_345L, day = LocalDate(2026, 8, 31))),
        )
        viewModel = accountsViewModel()
        advanceUntilIdle()

        assertEquals(YearMonth(2026, Month.AUGUST), viewModel.state.value.month)
        assertEquals("S/ 193.45", viewModel.state.value.monthSpent)

        today.value = LocalDate(2026, 9, 1)
        advanceUntilIdle()

        assertEquals(YearMonth(2026, Month.SEPTEMBER), viewModel.state.value.month)
        assertEquals("S/ 0.00", viewModel.state.value.monthSpent)
        assertEquals("S/ 0.00", rowFor(bcp).net)
    }

    @Test
    fun `an account with no movement at all still gets a row`() = runTest {
        every { accountRepository.all() } returns flowOf(listOf(bcp, cash))

        viewModel = accountsViewModel()
        advanceUntilIdle()

        assertEquals(listOf(bcp, cash), viewModel.state.value.accounts.map { it.account })
    }

    @Test
    fun `loansTotalOwed defaults to zero, unsigned, when there are no loans`() = runTest {
        advanceUntilIdle()

        assertEquals("S/ 0.00", viewModel.state.value.loansTotalOwed)
        assertEquals(emptyList(), viewModel.state.value.loansPeople)
    }

    @Test
    fun `loansTotalOwed is the signed sum of every PersonBalance remaining, not raw cents`() = runTest {
        every { loanRepository.balancesByPerson() } returns flowOf(
            listOf(
                personBalance(personKey = "juan", personName = "Juan", remaining = Money(25_000L)),
                personBalance(personKey = "maria", personName = "María", remaining = Money(10_000L)),
            ),
        )
        viewModel = accountsViewModel()

        advanceUntilIdle()

        assertEquals("+S/ 350.00", viewModel.state.value.loansTotalOwed)
        assertEquals(listOf("Juan", "María"), viewModel.state.value.loansPeople)
    }

    @Test
    fun `a settled person owes nothing and so names nobody`() = runTest {
        every { loanRepository.balancesByPerson() } returns flowOf(
            listOf(
                personBalance(personKey = "juan", personName = "Juan", remaining = Money.Zero),
                personBalance(personKey = "maria", personName = "María", remaining = Money(10_000L)),
            ),
        )
        viewModel = accountsViewModel()

        advanceUntilIdle()

        assertEquals(listOf("María"), viewModel.state.value.loansPeople)
    }

    @Test
    fun `loansTotalOwed updates when the loans flow re-emits, with no manual refresh`() = runTest {
        val loansFlow = MutableStateFlow<List<PersonBalance>>(emptyList())
        every { loanRepository.balancesByPerson() } returns loansFlow
        viewModel = accountsViewModel()
        advanceUntilIdle()

        assertEquals("S/ 0.00", viewModel.state.value.loansTotalOwed)

        loansFlow.value = listOf(personBalance(remaining = Money(50_000L)))
        advanceUntilIdle()

        assertEquals("+S/ 500.00", viewModel.state.value.loansTotalOwed)
    }

    @Test
    fun `no loan or abono moves an account net by one cent, however large the loans flow emits`() = runTest {
        // ADR 010: loans are a parallel ledger. balancesByPerson() and all() are two separate flows
        // over two different tables — a huge loans emission must leave every monthly net untouched.
        every { accountRepository.all() } returns flowOf(listOf(bcp))
        every { transactionRepository.all() } returns flowOf(
            listOf(movement("t-1", bcp, TransactionType.Spend, 19_345L)),
        )
        val loansFlow = MutableStateFlow<List<PersonBalance>>(emptyList())
        every { loanRepository.balancesByPerson() } returns loansFlow
        viewModel = accountsViewModel()
        advanceUntilIdle()

        assertEquals("−S/ 193.45", rowFor(bcp).net)

        loansFlow.value = listOf(personBalance(remaining = Money(999_999_999L)))
        advanceUntilIdle()

        assertEquals("−S/ 193.45", rowFor(bcp).net)
        assertEquals("S/ 193.45", viewModel.state.value.monthSpent)
        assertEquals("+S/ 9,999,999.99", viewModel.state.value.loansTotalOwed)
    }
}
