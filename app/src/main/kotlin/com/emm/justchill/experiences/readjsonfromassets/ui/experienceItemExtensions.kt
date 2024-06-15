package com.emm.justchill.experiences.readjsonfromassets.ui

import androidx.compose.ui.graphics.Color
import com.emm.justchill.experiences.readjsonfromassets.domain.Experience

private fun Experience.toUi() = ExperienceItemUiState(
    title = title,
    description = description,
    date = date,
    category = category,
    resource = resource,
    categoryColor = selectColorByCategory()
)

fun List<Experience>.toUi() = map(Experience::toUi)

private fun Experience.selectColorByCategory(): Color = if (category == "video") {
    Color.Blue
} else {
    Color.Green
}