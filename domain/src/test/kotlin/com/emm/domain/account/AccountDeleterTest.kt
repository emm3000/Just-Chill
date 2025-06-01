package com.emm.domain.account

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountDeleterTest {

    private val repository = mockk<AccountRepository>()
    private val accountDeleter = AccountDeleter(repository)

    @Test
    fun `delete should call repository deleteBy with correct accountId`() = runTest {

        coEvery { repository.delete(any()) } just Runs

        accountDeleter.delete("1234")

        coVerify(exactly = 1) { repository.delete("1234") }

    }

}