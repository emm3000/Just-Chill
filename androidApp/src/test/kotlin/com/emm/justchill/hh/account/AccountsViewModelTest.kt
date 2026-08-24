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
import com.emm.domain.transaction.TransactionRepository
import com.emm.justchill.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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

    private lateinit var viewModel: AccountsViewModel

    private fun accountsViewModel() = AccountsViewModel(
        accountRepository,
        transactionRepository,
        loanRepository,
        updateAccount,
        deleteAccount,
    )

    private fun personBalance(
        personKey: String = "juan",
        personName: String = "Juan",
        remaining: Money = Money(10_000L),
    ) = PersonBalance(personKey = personKey, personName = personName, remaining = remaining)

    @Before
    fun setUp() {
        every { accountRepository.all() } returns flowOf(emptyList())
        every { transactionRepository.all() } returns flowOf(emptyList())
        every { loanRepository.balancesByPerson() } returns flowOf(emptyList())
        viewModel = accountsViewModel()
    }

    @Test
    fun `loansTotalOwed defaults to zero, formatted, when there are no loans`() = runTest {
        advanceUntilIdle()

        assertEquals("S/ 0.00", viewModel.state.value.loansTotalOwed)
    }

    @Test
    fun `loansTotalOwed is the formatted sum of every PersonBalance remaining, not raw cents`() = runTest {
        every { loanRepository.balancesByPerson() } returns flowOf(
            listOf(
                personBalance(personKey = "juan", remaining = Money(25_000L)),
                personBalance(personKey = "maria", remaining = Money(10_000L)),
            ),
        )
        viewModel = accountsViewModel()

        advanceUntilIdle()

        assertEquals("S/ 350.00", viewModel.state.value.loansTotalOwed)
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

        assertEquals("S/ 500.00", viewModel.state.value.loansTotalOwed)
    }

    @Test
    fun `accounts stay whatever the account flow emitted, regardless of the loans flow`() = runTest {
        val account = Account(accountId = AccountId("acc-1"), name = "BCP", type = AccountType.Bank)
        every { accountRepository.all() } returns flowOf(listOf(account))
        every { loanRepository.balancesByPerson() } returns flowOf(
            listOf(personBalance(remaining = Money(999_999L))),
        )
        viewModel = accountsViewModel()

        advanceUntilIdle()

        assertEquals(listOf(account), viewModel.state.value.accounts)
    }
}
