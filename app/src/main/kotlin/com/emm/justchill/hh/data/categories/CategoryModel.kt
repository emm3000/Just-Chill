package com.emm.justchill.hh.data.categories

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryModel(

    @SerialName("category_id")
    val categoryId: String,

    val name: String,

    val description: String,

    val type: String,

    @SerialName("user_id")
    val userId: String = "",
)