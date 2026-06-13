package com.emm.data.backup

import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val categoryId: String,
    val name: String,
    val icon: String,
    val color: String,
    val categoryType: String,
)

fun Category.toDto() = CategoryDto(
    categoryId = categoryId.value,
    name = name,
    icon = icon,
    color = color,
    categoryType = categoryType.name,
)

fun CategoryDto.toEntity() = Category(
    categoryId = CategoryId(categoryId),
    name = name,
    icon = icon,
    color = color,
    categoryType = CategoryType.valueOf(categoryType),
)
