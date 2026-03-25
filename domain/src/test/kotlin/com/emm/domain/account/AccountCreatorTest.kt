package com.emm.domain.account

import com.emm.domain.shared.UniqueIdProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountCreatorTest {

    private val repository = mockk<AccountRepository>()
    private val uniqueIdProvider = mockk<UniqueIdProvider>()
    private val accountCreator = AccountCreator(repository, uniqueIdProvider)

    @Test
    fun `create should call repository create with correct accountUpsert`() = runTest {
        every { uniqueIdProvider.id } returns "test-id"
        coEvery { repository.create(any()) } just Runs

        accountCreator.create(name = "Test Account")

        coVerify(exactly = 1) { repository.create(any()) }

        confirmVerified(repository)
    }
}
