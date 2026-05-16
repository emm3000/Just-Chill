package com.emm.domain.account

import com.emm.domain.shared.AccountId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertNull

class AccountFinderTest {

    private val repository = mockk<AccountRepository>()
    private val accountFinder = FindAccountUseCase(repository)

    @Test
    fun `find should call repository findBy with correct accountId`() = runTest {

        coEvery { repository.find(any()) } returns null

        val find: Account? = accountFinder(AccountId("1234"))

        assertNull(find)

        coVerify(exactly = 1) { repository.find(AccountId("1234")) }
    }
}
