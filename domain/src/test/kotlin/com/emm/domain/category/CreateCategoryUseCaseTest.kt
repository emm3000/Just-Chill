package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class CreateCategoryUseCaseTest {

    private val repository = mockk<CategoryRepository>()
    private val idProvider = mockk<UniqueIdProvider>()
    private val useCase = CreateCategoryUseCase(repository, idProvider)

    @Test
    fun `create should call repository with generated id and mapped fields`() = runTest {
        every { idProvider.id } returns "cat-1"
        coEvery { repository.create(any()) } just Runs

        useCase(
            name = "Food",
            icon = "icon-1",
            color = "red",
            categoryType = CategoryType.Spend,
        )

        coVerify(exactly = 1) {
            repository.create(
                CategoryUpsert(
                    categoryId = CategoryId("cat-1"),
                    name = "Food",
                    icon = "icon-1",
                    color = "red",
                    categoryType = CategoryType.Spend,
                ),
            )
        }
    }

    @Test
    fun `create should always use idProvider for categoryId`() = runTest {
        every { idProvider.id } returns "fresh-id"
        coEvery { repository.create(any()) } just Runs

        useCase(name = "n", icon = "i", color = "c", categoryType = CategoryType.Income)

        coVerify {
            repository.create(match { it.categoryId == CategoryId("fresh-id") })
        }
    }

    @Test
    fun `create should propagate DomainException from repository`() = runTest {
        every { idProvider.id } returns "id"
        coEvery { repository.create(any()) } throws DomainException.DatabaseError(RuntimeException("boom"))

        assertFailsWith<DomainException.DatabaseError> {
            useCase(name = "n", icon = "i", color = "c", categoryType = CategoryType.Income)
        }
    }
}
