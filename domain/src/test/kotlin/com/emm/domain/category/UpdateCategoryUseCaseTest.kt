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

class UpdateCategoryUseCaseTest {

    private val repository = mockk<CategoryRepository>()
    private val useCase = UpdateCategoryUseCase(repository)

    private val anyUpsert = CategoryUpsert(
        categoryId = CategoryId("cat-1"),
        name = "Food",
        icon = "icon",
        color = "blue",
        categoryType = CategoryType.Spend,
    )

    @Test
    fun `update should call repository with given id and upsert`() = runTest {
        coEvery { repository.update(any(), any()) } just Runs

        useCase(CategoryId("cat-1"), anyUpsert)

        coVerify(exactly = 1) { repository.update(CategoryId("cat-1"), anyUpsert) }
    }

    @Test
    fun `update should propagate DomainException from repository`() = runTest {
        coEvery { repository.update(any(), any()) } throws DomainException.NotFound("category")

        assertFailsWith<DomainException.NotFound> { useCase(CategoryId("cat-1"), anyUpsert) }
    }
}
