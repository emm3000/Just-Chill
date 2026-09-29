package com.emm.justchill.feature.category

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.ui.theme.EmmTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CategoriesScreenTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private var addCategoryClicks: Int = 0

    private val oneCategory: List<Category> = listOf(
        Category(CategoryId("1"), "Sueldo", "wallet", "green", CategoryType.Income),
    )

    @Test
    fun `renders the create category call to action with a click action`() {
        renderCategories(categories = emptyList())

        composeRule.onNodeWithText("Crear categoría").assertHasClickAction()
    }

    @Test
    fun `announces the create category call to action as a button`() {
        renderCategories(categories = emptyList())

        composeRule.onNodeWithText("Crear categoría")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `clicking the create category call to action invokes onAddCategory exactly once`() {
        renderCategories(categories = emptyList())

        composeRule.onNodeWithText("Crear categoría").performClick()

        assertEquals(1, addCategoryClicks)
    }

    @Test
    fun `spans the content width less the empty state's horizontal padding`() {
        renderCategories(categories = emptyList())

        composeRule.onNodeWithText("Crear categoría").assertWidthIsEqualTo(371.dp)
    }

    @Test
    fun `hides the create category call to action once a category exists`() {
        renderCategories(categories = oneCategory)

        composeRule.onNodeWithText("Crear categoría").assertDoesNotExist()
    }

    @Test
    fun `keeps the new category top bar action once a category exists`() {
        renderCategories(categories = oneCategory)

        composeRule.onNodeWithContentDescription("Nueva categoría").assertHasClickAction()
    }

    private fun renderCategories(categories: List<Category>) {
        composeRule.setContent {
            EmmTheme {
                CategoriesScreen(
                    state = CategoriesUiState(categories = categories),
                    onIntent = {},
                    onAddCategory = { addCategoryClicks += 1 },
                    onBack = {},
                )
            }
        }
    }
}
