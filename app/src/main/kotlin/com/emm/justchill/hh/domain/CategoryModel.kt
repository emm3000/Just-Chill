package com.emm.justchill.hh.domain

import com.emm.justchill.Categories
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryModel(

    @SerialName("category_id")
    val categoryId: String,

    val name: String,
    val type: String,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("updated_at")
    val updatedAt: Long,

    @SerialName("device_id")
    val deviceId: String = "",

    @SerialName("device_name")
    val deviceName: String = "",

    @SerialName("user_id")
    val userId: String = "",
)

fun Categories.toModel() = CategoryModel(
    categoryId = categoryId,
    name = name,
    type = type,
    createdAt = createdAt,
    updatedAt = updatedAt
)