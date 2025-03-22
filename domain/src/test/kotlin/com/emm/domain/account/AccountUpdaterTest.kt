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
    private val accountUpdater = AccountUpdater(repository)

    @Test
    fun `update should call repository update with correct accountId and accountUpsert`() = runTest {

        coEvery { repository.update(any(), any()) } just Runs

        val account = AccountUpsert(
            name = "Test Account",
            balance = 100.0,
            description = "Test description",
            isSelected = AccountSelect.NonSelected,
        )
        accountUpdater.update("123", account)

        coVerify(exactly = 1) { repository.update("123", account) }
    }
}