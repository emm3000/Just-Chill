package com.emm.justchill.experiences.drinks.ui

import com.emm.justchill.experiences.drinks.data.DrinkApiModel
import kotlinx.serialization.Serializable

@Serializable
object DrinkList

@Serializable
data class DrinkDetail(
    val title: String,
    val description: String,
    val image: String
)

fun DrinkApiModel.toRoute() = DrinkDetail(
    title = name,
    description = description,
    image = image
)