package com.emm.justchill.core

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.experiences.calendar.Calendar
import com.emm.justchill.experiences.drinks.ui.DrinkMainScreen
import com.emm.justchill.experiences.readjsonfromassets.ui.Experiences
import com.emm.justchill.experiences.supabase.SupabaseScreen
import com.emm.justchill.experiences.timerpicker.MeTimerPicker
import com.emm.justchill.hh.shared.Hh
import java.util.UUID

sealed class RootRoutes(val route: String) {

    data object ExperiencesRoot : RootRoutes("ExperiencesRoot")
    data object Experiences : RootRoutes("Experiences")
    data object Drink : RootRoutes("Drink")
    data object Hh : RootRoutes("hh")
    data object Amount : RootRoutes("amount")
    data object Calendar : RootRoutes("Calendar")
    data object MeTimerPicker : RootRoutes("MeTimerPicker")
}

val rootData: List<Feature> = listOf(
    Feature(
        id = UUID.randomUUID().toString(),
        title = "Simple list and detail from drinks api",
        description = "In this part, it was used jetpack compose components, (LazyColumn), In this part, it was used jetpack compose components, (LazyColumn)",
        category = "video",
        resource = "no link",
        route = RootRoutes.Experiences,
        screen = { DrinkMainScreen() }
    ),
    Feature(
        id = UUID.randomUUID().toString(),
        title = "Simple clean architecture, the data is from local assets in json format",
        description = "In this example Kotlin was used",
        category = "blog",
        resource = "no link",
        route = RootRoutes.Drink,
        screen = { Experiences(it) }
    ),
    Feature(
        id = UUID.randomUUID().toString(),
        title = "Understanding Margin and Padding in Compose UI",
        description = "How work padding in Jetpack compose and margin with padding",
        category = "blog",
        resource = "no link",
        route = RootRoutes.Drink,
        screen = { Experiences(it) }
    ),
    Feature(
        id = UUID.randomUUID().toString(),
        title = "Create finance app, with",
        description = "Create complete app, after change the styles",
        category = "personal",
        resource = "-",
        route = RootRoutes.Hh,
        screen = { Hh() }
    ),
    Feature(
        id = UUID.randomUUID().toString(),
        title = "Supabase",
        description = "Tab to test supabase",
        category = "personal",
        resource = "-",
        route = RootRoutes.Hh,
        screen = { SupabaseScreen() }
    ),
    Feature(
        id = UUID.randomUUID().toString(),
        title = "Amount component",
        description = "Create new component in jetpack compose like bcp app to add amount",
        category = "personal",
        resource = "-",
        route = RootRoutes.Amount,
        screen = {
            Column(Modifier.fillMaxSize()) {
                EmmAmountChill(
                    value = TextFieldValue(),
                    onValueChange = {
                        Log.e("aea", it.toString())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ),
    Feature(
        id = UUID.randomUUID().toString(),
        title = "Calendar Component",
        description = "Calendar Component",
        category = "personal",
        resource = "-",
        route = RootRoutes.Calendar,
        screen = { Calendar() }
    ),
    Feature(
        id = UUID.randomUUID().toString(),
        title = "NewComponent timer picker",
        description = "Timer picker",
        category = "personal",
        resource = "-",
        route = RootRoutes.MeTimerPicker,
        screen = { MeTimerPicker() }
    )
).reversed()