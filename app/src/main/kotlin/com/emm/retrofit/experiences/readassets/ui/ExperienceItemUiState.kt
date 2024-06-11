package com.emm.retrofit.experiences.readassets.ui

import androidx.compose.ui.graphics.Color
import com.emm.retrofit.experiences.readassets.domain.Experience

data class ExperienceItemUiState(
    val title: String,
    val description: String,
    val date: String,
    val category: String,
    val resource: String,
    val categoryColor: Color
)

private fun Experience.toUi() = ExperienceItemUiState(
    title = title,
    description = description,
    date = date,
    category = category,
    resource = resource,
    categoryColor = selectColorByCategory()
)

fun List<Experience>.toUi() = map(Experience::toUi)

private fun Experience.selectColorByCategory(): Color = if (resource == "video") {
    Color.Blue
} else {
    Color.Green
}
