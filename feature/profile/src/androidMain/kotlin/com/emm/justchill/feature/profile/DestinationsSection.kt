package com.emm.justchill.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.People
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal data class ProfileDestinationActions(val onCategoriesClick: () -> Unit, val onLoansClick: () -> Unit)

@Composable
internal fun DestinationsSection(
    categoryCount: Int,
    incomeCategoryCount: Int,
    destinations: ProfileDestinationActions,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = "Registro")
        ProfileGroup {
            ProfileRow(
                icon = Icons.Outlined.Category,
                label = "Categorías",
                meta = categoriesMetaText(categoryCount, incomeCategoryCount),
                metaIsPrimary = false,
                onClick = destinations.onCategoriesClick,
            )
            ProfileRow(
                icon = Icons.Outlined.People,
                label = "Préstamos",
                meta = "",
                metaIsPrimary = false,
                onClick = destinations.onLoansClick,
            )
        }
    }
}
