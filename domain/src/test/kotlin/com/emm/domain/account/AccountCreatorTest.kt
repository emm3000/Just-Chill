package com.emm.domain.account

import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class AccountCreatorTest {

    private val repository = mockk<AccountRepository>()
    private val uniqueIdProvider = mockk<UniqueIdProvider>()
    private val accountCreator = CreateAccountUseCase(repository, uniqueIdProvider)

    @Test
    fun `create should call repository create with correct accountUpsert`() = runTest {
        every { uniqueIdProvider.id } returns "test-id"
        coEvery { repository.create(any()) } just Runs

        accountCreator(name = "Test Account")

        coVerify(exactly = 1) { repository.create(any()) }

        confirmVerified(repository)
    }

    @Test
    fun `create should throw ValidationError when name is empty`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            accountCreator(name = "")
        }
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `create should throw ValidationError when name is blank`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            accountCreator(name = "   ")
        }
        coVerify(exactly = 0) { repository.create(any()) }
    }
}
