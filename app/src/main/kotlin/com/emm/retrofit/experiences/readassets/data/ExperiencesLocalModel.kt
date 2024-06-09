package com.emm.retrofit.experiences.readassets.data

import com.emm.retrofit.experiences.readassets.domain.Experience
import kotlinx.serialization.Serializable

@Serializable
data class ExperiencesLocalModel(
    val title: String,
    val description: String,
    val date: String,
    val category: String,
    val resource: String,
)

private fun ExperiencesLocalModel.toDomain() = Experience(
    title = title,
    description = description,
    date = date,
    category = category,
    resource = resource
)

fun List<ExperiencesLocalModel>.toDomain(): List<Experience> = map(ExperiencesLocalModel::toDomain)