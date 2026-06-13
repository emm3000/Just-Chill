package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.error.DomainException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FindCategoryUseCaseTest {

    private val repository = mockk<CategoryRepository>()
    private val useCase = FindCategoryUseCase(repository)

    private val category = Category(
        categoryId = CategoryId("cat-1"),
        name = "Food",
        icon = "icon",
        color = "red",
        categoryType = CategoryType.Spend,
    )

    @Test
    fun `find should return category when repository returns one`() = runTest {
        coEvery { repository.find(CategoryId("cat-1")) } returns category

        val result = useCase(CategoryId("cat-1"))

        assertEquals(category, result)
    }

    @Test
    fun `find should return null when repository returns null`() = runTest {
        coEvery { repository.find(any()) } returns null

        assertNull(useCase(CategoryId("missing")))
    }

    @Test
    fun `find should propagate DomainException from repository`() = runTest {
        coEvery { repository.find(any()) } throws DomainException.DatabaseError(RuntimeException("boom"))

        assertFailsWith<DomainException.DatabaseError> { useCase(CategoryId("cat-1")) }
    }
}
