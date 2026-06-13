package com.emm.justchill.experiences

import com.emm.justchill.core.Feature
import com.emm.justchill.experiences.readjsonfromassets.ui.ExperienceItemUiState
import java.time.LocalDate

data class ExperienceBundle(
    val title: String,
    val description: String,
    val date: String,
    val route: String,
    val category: String,
    val resource: String,
)

fun Feature.toBundle() = ExperienceBundle(
    title = title,
    description = description,
    route = route.route,
    date = LocalDate.now().toString(),
    category = category,
    resource = resource
)

fun ExperienceItemUiState.toBundle() = ExperienceBundle(
    title = title,
    description = description,
    date = date,
    route = "",
    category = category,
    resource = resource

)