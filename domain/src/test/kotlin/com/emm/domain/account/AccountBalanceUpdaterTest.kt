package com.emm.domain.account

import com.emm.domain.transaction.TransactionType
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountBalanceUpdaterTest {

    private val accountRepository = FakeAccountRepository()
    private val accountUpdateRepository = mockk<AccountUpdateRepository>()
    private val accountBalanceUpdater = AccountBalanceUpdater(accountRepository, accountUpdateRepository)

    companion object {

        val randomAccount = listOf(
            Account(
                accountId = "1",
                name = "Test Account",
                balance = 100.0,
                description = "Test description",
                isSelected = AccountSelect.NonSelected,
            )
        )
    }

    @Test
    fun `update should not update balance if account does not exist`() = runTest {

        coEvery { accountUpdateRepository.updateAmount(any(), any()) } just Runs

        accountRepository.send(randomAccount)

        accountBalanceUpdater.update(
            accountId = "2",
            transactionType = TransactionType.Income,
            amount = 10.0
        )

        coVerify(exactly = 1) { accountUpdateRepository.updateAmount(any(), any()) }
    }

    @Test
    fun `update should add amount for income transactions`() = runTest {
        // Arrange
        val accountTest: Account = randomAccount.first()
        val amountToAdd = 100.0
        val expectedNewBalance = accountTest.balance + amountToAdd

        coEvery { accountUpdateRepository.updateAmount(accountTest.accountId, expectedNewBalance) } just Runs

        accountRepository.send(randomAccount)

        // Act
        accountBalanceUpdater.update(accountTest.accountId, TransactionType.Income, amountToAdd)

        // Assert
        coVerify(exactly = 1) { accountUpdateRepository.updateAmount(accountTest.accountId, expectedNewBalance) }
    }

    @Test
    fun `update should subtract amount for spend transactions`() = runTest {
        // Arrange
        val accountTest: Account = randomAccount.first()
        val amountToAdd = 100.0
        val expectedNewBalance = accountTest.balance - amountToAdd

        coEvery { accountUpdateRepository.updateAmount(accountTest.accountId, expectedNewBalance) } just Runs

        accountRepository.send(randomAccount)

        // Act
        accountBalanceUpdater.update(accountTest.accountId, TransactionType.Spend, amountToAdd)

        // Assert
        coVerify(exactly = 1) { accountUpdateRepository.updateAmount(accountTest.accountId, expectedNewBalance) }
    }
}