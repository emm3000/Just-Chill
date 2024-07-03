package com.emm.justchill.hh.domain

import com.emm.justchill.Categories
import kotlinx.serialization.Serializable

@Serializable
data class CategoryModel(
    val categoryId: String,
    val name: String,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun Categories.toModel() = CategoryModel(
    categoryId = categoryId,
    name = name,
    type = type,
    createdAt = createdAt,
    updatedAt = updatedAt
)