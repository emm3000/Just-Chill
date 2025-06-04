package com.emm.data.category

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryModel(

    @SerialName("category_id")
    val categoryId: String,

    val name: String,

    @SerialName("updated_at")
    val updatedAt: Long,

    @SerialName("user_id")
    val userId: String = "",
)