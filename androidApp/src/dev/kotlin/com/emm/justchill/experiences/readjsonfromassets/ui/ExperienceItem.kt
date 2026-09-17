package com.emm.justchill.experiences.readjsonfromassets.ui

import androidx.compose.ui.graphics.Color

data class ExperienceItem(
    val title: String,
    val description: String,
    val date: String,
    val category: String,
    val resource: String,
    val categoryColor: Color,
)
