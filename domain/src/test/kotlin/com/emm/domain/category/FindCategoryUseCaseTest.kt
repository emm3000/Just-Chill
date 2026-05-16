package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.error.DomainException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    fun `find should return first element from repository flow`() = runTest {
        every { repository.find(CategoryId("cat-1")) } returns flowOf(category)

        val result = useCase(CategoryId("cat-1"))

        assertEquals(category, result)
    }

    @Test
    fun `find should return null when flow emits null`() = runTest {
        every { repository.find(any()) } returns flowOf(null)

        assertNull(useCase(CategoryId("missing")))
    }

    @Test
    fun `find should propagate DomainException from repository flow`() = runTest {
        every { repository.find(any()) } returns flow { throw DomainException.DatabaseError(RuntimeException("boom")) }

        assertFailsWith<DomainException.DatabaseError> { useCase(CategoryId("cat-1")) }
    }
}
