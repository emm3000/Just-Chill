package com.emm.justchill.core.ui.transaction

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.ui.category.SelectableCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoriesOfTest {

    private fun selectable(
        id: String,
        categoryType: CategoryType = CategoryType.Spend,
        name: String = id,
    ): SelectableCategory = SelectableCategory(
        categoryId = CategoryId(id),
        name = name,
        iconId = "cart",
        categoryType = categoryType,
        colorId = "green",
    )

    private fun loaded(categories: Map<CategoryType, List<SelectableCategory>> = emptyMap()): Catalog.Loaded =
        Catalog.Loaded(accounts = emptyList(), categories = categories)

    @Test
    fun `loaded rows keep their order`() {
        val first: SelectableCategory = selectable("cat-1")
        val second: SelectableCategory = selectable("cat-2")
        val catalog: Catalog.Loaded = loaded(mapOf(CategoryType.Spend to listOf(first, second)))

        val result: List<SelectableCategory> = catalog.categoriesOf(CategoryType.Spend, extras = emptyList())

        assertEquals(listOf(first, second), result)
    }

    @Test
    fun `an extra of the other type is dropped`() {
        val known: SelectableCategory = selectable("cat-1", categoryType = CategoryType.Spend)
        val otherTypeExtra: SelectableCategory = selectable("extra-1", categoryType = CategoryType.Income)
        val catalog: Catalog.Loaded = loaded(mapOf(CategoryType.Spend to listOf(known)))

        val result: List<SelectableCategory> = catalog.categoriesOf(
            CategoryType.Spend,
            extras = listOf(otherTypeExtra),
        )

        assertEquals(listOf(known), result)
    }

    @Test
    fun `an extra whose id is already loaded is dropped`() {
        val known: SelectableCategory = selectable("cat-1", name = "Loaded name")
        val staleExtra: SelectableCategory = selectable("cat-1", name = "Stale name")
        val catalog: Catalog.Loaded = loaded(mapOf(CategoryType.Spend to listOf(known)))

        val result: List<SelectableCategory> = catalog.categoriesOf(
            CategoryType.Spend,
            extras = listOf(staleExtra),
        )

        assertEquals(listOf(known), result)
    }

    @Test
    fun `surviving extras come first`() {
        val known: SelectableCategory = selectable("cat-1")
        val extra: SelectableCategory = selectable("extra-1")
        val catalog: Catalog.Loaded = loaded(mapOf(CategoryType.Spend to listOf(known)))

        val result: List<SelectableCategory> = catalog.categoriesOf(
            CategoryType.Spend,
            extras = listOf(extra),
        )

        assertEquals(listOf(extra, known), result)
    }

    @Test
    fun `Loading carries extras until the catalog arrives`() {
        val extra: SelectableCategory = selectable("extra-1")

        val result: List<SelectableCategory> = Catalog.Loading.categoriesOf(CategoryType.Spend, extras = listOf(extra))

        assertEquals(listOf(extra), result)
    }
}
