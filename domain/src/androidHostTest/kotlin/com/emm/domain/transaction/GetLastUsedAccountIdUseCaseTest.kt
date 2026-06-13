package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetLastUsedAccountIdUseCaseTest {

    private val repo = mockk<TransactionStatsRepository>()
    private val useCase = GetLastUsedAccountIdUseCase(repo)

    @Test
    fun `returns last-used accountId from repo`() = runTest {
        val expected = AccountId("bcp")
        coEvery { repo.lastUsedAccountId() } returns expected

        val result = useCase()

        assertEquals(expected, result)
    }

    @Test
    fun `returns null when repo returns null (no history)`() = runTest {
        coEvery { repo.lastUsedAccountId() } returns null

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `delegates to repo exactly once`() = runTest {
        coEvery { repo.lastUsedAccountId() } returns null

        useCase()

        coVerify(exactly = 1) { repo.lastUsedAccountId() }
    }
}
