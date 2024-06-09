package com.emm.retrofit.experiences.drinks.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DrinkApiModel(

    @SerialName("idDrink")
    val id: String = "",

    @SerialName("strDrinkThumb")
    val image: String = "",

    @SerialName("strDrink")
    val name: String = "",

    @SerialName("strInstructions")
    val description: String = ""
)

@Serializable
data class DrinkListApiModel(

    @SerialName("drinks") val drinkApiModelList: List<DrinkApiModel>
)