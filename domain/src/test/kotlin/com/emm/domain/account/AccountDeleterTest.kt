package com.emm.domain.account

import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class AccountDeleterTest {

    private val repository = mockk<AccountRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val recurringMovementRepository = mockk<RecurringMovementRepository>()
    private val accountDeleter = DeleteAccountUseCase(
        repository,
        transactionRepository,
        recurringMovementRepository,
    )

    @Test
    fun `delete should call repository delete when account has no live transactions or recurring`() = runTest {
        coEvery { transactionRepository.countLiveByAccount(any()) } returns 0L
        coEvery { recurringMovementRepository.countLiveByAccount(any()) } returns 0L
        coEvery { repository.delete(any()) } just Runs

        accountDeleter(AccountId("1234"))

        coVerify(exactly = 1) { repository.delete(AccountId("1234")) }
    }

    @Test
    fun `delete should throw ValidationError when account has live transactions`() = runTest {
        coEvery { transactionRepository.countLiveByAccount(any()) } returns 3L
        coEvery { recurringMovementRepository.countLiveByAccount(any()) } returns 0L

        assertFailsWith<DomainException.ValidationError> {
            accountDeleter(AccountId("1234"))
        }
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `delete should succeed when account has only tombstoned transactions`() = runTest {
        // countLiveByAccount filters deletedAt IS NULL — tombstoned rows return 0.
        coEvery { transactionRepository.countLiveByAccount(any()) } returns 0L
        coEvery { recurringMovementRepository.countLiveByAccount(any()) } returns 0L
        coEvery { repository.delete(any()) } just Runs

        accountDeleter(AccountId("acc-tombstoned"))

        coVerify(exactly = 1) { repository.delete(AccountId("acc-tombstoned")) }
    }

    @Test
    fun `delete should throw ValidationError when account has live recurring movements`() = runTest {
        coEvery { transactionRepository.countLiveByAccount(any()) } returns 0L
        coEvery { recurringMovementRepository.countLiveByAccount(any()) } returns 2L

        assertFailsWith<DomainException.ValidationError> {
            accountDeleter(AccountId("1234"))
        }
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `delete ValidationError message is descriptive`() = runTest {
        coEvery { transactionRepository.countLiveByAccount(any()) } returns 1L
        coEvery { recurringMovementRepository.countLiveByAccount(any()) } returns 0L

        val ex = assertFailsWith<DomainException.ValidationError> {
            accountDeleter(AccountId("acc-1"))
        }
        assert(ex.message?.isNotBlank() == true)
    }
}
