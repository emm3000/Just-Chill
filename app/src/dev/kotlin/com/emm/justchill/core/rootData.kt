package com.emm.justchill.core

import com.emm.justchill.experiences.drinks.ui.DrinkMainScreen
import com.emm.justchill.experiences.readjsonfromassets.ui.Experiences
import com.emm.justchill.experiences.supabase.SupabaseScreen
import com.emm.justchill.hh.presentation.Hh
import java.util.UUID

sealed class RootRoutes(val route: String) {

    data object ExperiencesRoot : RootRoutes("ExperiencesRoot")
    data object Experiences : RootRoutes("Experiences")
    data object Drink : RootRoutes("Drink")
    data object Hh : RootRoutes("hh")
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
    )
)