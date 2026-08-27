package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class DeleteCategoryUseCaseTest {

    private val repository = mockk<CategoryRepository>()
    private val useCase = DeleteCategoryUseCase(repository)

    @Test
    fun `delete tombstones the category and nothing else`() = runTest {
        coEvery { repository.delete(any()) } just Runs

        useCase(CategoryId("cat-1"))

        coVerify(exactly = 1) { repository.delete(CategoryId("cat-1")) }
    }

    @Test
    fun `delete should propagate DomainException from repository`() = runTest {
        coEvery { repository.delete(any()) } throws DomainException.DatabaseError(RuntimeException("nope"))

        assertFailsWith<DomainException.DatabaseError> { useCase(CategoryId("cat-1")) }
    }
}
