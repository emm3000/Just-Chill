package com.emm.domain.account

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountCreatorTest {

    private val repository = mockk<AccountRepository>()
    private val accountCreator = AccountCreator(repository)

    @Test
    fun `create should call repository create with correct accountUpsert`() = runTest {
        val testAccount = AccountUpsert(
            name = "Test Account",
            balance = 100.0,
            description = "Test description",
            isSelected = AccountSelect.NonSelected,
        )

        coEvery { repository.create(testAccount) } just Runs

        accountCreator.create(testAccount)

        coVerify(exactly = 1) { repository.create(testAccount) }

        confirmVerified(repository)
    }
}