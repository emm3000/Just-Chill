package com.emm.domain.account

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountUpdaterTest {

    private val repository = mockk<AccountUpdateRepository>()
    private val accountUpdater = UpdateAccountUseCase(repository)

    @Test
    fun `update should call repository update with correct accountId and accountUpsert`() = runTest {

        coEvery { repository.update(any(), any()) } just Runs

        val account = AccountUpsert(
            accountId = "123",
            name = "Test Account",
        )
        accountUpdater("123", account)

        coVerify(exactly = 1) { repository.update("123", account) }
    }
}
