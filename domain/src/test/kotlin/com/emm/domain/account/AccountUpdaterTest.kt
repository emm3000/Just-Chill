package com.emm.domain.account

import com.emm.domain.shared.AccountId
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountUpdaterTest {

    private val repository = mockk<AccountRepository>()
    private val accountUpdater = UpdateAccountUseCase(repository)

    @Test
    fun `update should call repository update with correct accountId and accountUpsert`() = runTest {

        coEvery { repository.update(any(), any()) } just Runs

        val account = AccountUpsert(
            accountId = AccountId("123"),
            name = "Test Account",
        )
        accountUpdater(AccountId("123"), account)

        coVerify(exactly = 1) { repository.update(AccountId("123"), account) }
    }
}
