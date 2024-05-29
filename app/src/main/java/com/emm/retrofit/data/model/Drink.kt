package com.emm.retrofit.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Drink(
    @SerialName("idDrink") val id: String = "",
    @SerialName("strDrinkThumb") val image: String = "",
    @SerialName("strDrink") val name: String = "",
    @SerialName("strInstructions") val description: String = ""
): Parcelable

@Serializable
data class DrinkList(@SerialName("drinks") val drinkList: List<Drink>)