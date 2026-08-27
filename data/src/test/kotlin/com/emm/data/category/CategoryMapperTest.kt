package com.emm.data.category

import com.emm.domain.category.CategoryType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CategoryMapperTest {

    @Test
    fun `asExternalModelOrNull - valid Income type returns Category`() {
        val entity = categoryEntity("cat-1", "Income")
        val result = entity.asExternalModelOrNull()
        assertNotNull(result)
        assertEquals(CategoryType.Income, result.categoryType)
        assertEquals("cat-1", result.categoryId.value)
    }

    @Test
    fun `asExternalModelOrNull - valid Spend type returns Category`() {
        val entity = categoryEntity("cat-1", "Spend")
        val result = entity.asExternalModelOrNull()
        assertNotNull(result)
        assertEquals(CategoryType.Spend, result.categoryType)
    }

    @Test
    fun `asExternalModelOrNull - unknown type INCOME returns null`() {
        assertNull(categoryEntity("cat-1", "INCOME").asExternalModelOrNull())
    }

    @Test
    fun `asExternalModelOrNull - unknown type lowercase income returns null`() {
        assertNull(categoryEntity("cat-1", "income").asExternalModelOrNull())
    }

    @Test
    fun `asExternalModelOrNull - completely unknown type returns null`() {
        assertNull(categoryEntity("cat-1", "Budget").asExternalModelOrNull())
    }

    @Test
    fun `list asExternalModel - unknown categoryType rows are skipped`() {
        val entities = listOf(
            categoryEntity("cat-good-1", "Income"),
            categoryEntity("cat-bad", "INCOME"),
            categoryEntity("cat-good-2", "Spend"),
        )
        val result = entities.asExternalModel()
        assertEquals(2, result.size)
        assertEquals("cat-good-1", result[0].categoryId.value)
        assertEquals("cat-good-2", result[1].categoryId.value)
    }

    @Test
    fun `list asExternalModel - all-bad list returns empty list`() {
        val entities = listOf(
            categoryEntity("cat-1", "INCOME"),
            categoryEntity("cat-2", "SPEND"),
        )
        assertEquals(emptyList(), entities.asExternalModel())
    }

    private fun categoryEntity(id: String, type: String) = CategoryEntity(
        categoryId = id,
        name = "Test",
        icon = "icon",
        color = "#000000",
        categoryType = type,
        isDefault = false,
        updatedAt = 0L,
        createdAt = 0L,
    )
}
