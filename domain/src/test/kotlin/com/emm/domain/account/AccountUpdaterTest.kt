package com.emm.domain.account

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

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

    @Test
    fun `update should throw ValidationError when name is empty`() = runTest {
        val account = AccountUpsert(accountId = AccountId("123"), name = "")
        assertFailsWith<DomainException.ValidationError> {
            accountUpdater(AccountId("123"), account)
        }
        coVerify(exactly = 0) { repository.update(any(), any()) }
    }

    @Test
    fun `update should throw ValidationError when name is blank`() = runTest {
        val account = AccountUpsert(accountId = AccountId("123"), name = "   ")
        assertFailsWith<DomainException.ValidationError> {
            accountUpdater(AccountId("123"), account)
        }
        coVerify(exactly = 0) { repository.update(any(), any()) }
    }
}
