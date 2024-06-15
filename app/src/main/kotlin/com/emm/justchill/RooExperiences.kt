package com.emm.justchill

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.emm.justchill.core.Feature
import com.emm.justchill.core.rootData
import com.emm.justchill.shared.ExperienceItem
import com.emm.justchill.shared.toBundle

@Composable
fun RootExperiences(navController: NavController) {

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {

        items(rootData, key = Feature::id) { feature ->
            ExperienceItem(data = feature.toBundle()) { bundle ->
                navController.navigate(bundle.route)
            }
        }
    }
}