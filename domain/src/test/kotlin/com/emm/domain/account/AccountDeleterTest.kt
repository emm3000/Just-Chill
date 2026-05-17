package com.emm.domain.account

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
    private val accountDeleter = DeleteAccountUseCase(repository, transactionRepository)

    @Test
    fun `delete should call repository delete when account has no transactions`() = runTest {
        coEvery { transactionRepository.countByAccount(any()) } returns 0L
        coEvery { repository.delete(any()) } just Runs

        accountDeleter(AccountId("1234"))

        coVerify(exactly = 1) { repository.delete(AccountId("1234")) }
    }

    @Test
    fun `delete should throw ValidationError when account has transactions`() = runTest {
        coEvery { transactionRepository.countByAccount(any()) } returns 3L

        assertFailsWith<DomainException.ValidationError> {
            accountDeleter(AccountId("1234"))
        }
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `delete ValidationError message matches expected spanish text`() = runTest {
        coEvery { transactionRepository.countByAccount(any()) } returns 1L

        val ex = assertFailsWith<DomainException.ValidationError> {
            accountDeleter(AccountId("acc-1"))
        }
        assert(ex.message == "No puedes eliminar una cuenta con transacciones. Bórralas o muévelas primero.")
    }
}
