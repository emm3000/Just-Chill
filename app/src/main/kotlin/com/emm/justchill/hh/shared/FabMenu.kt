package com.emm.justchill.hh.shared

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.shared.AddTransactionRoute

@Composable
fun FabMenu(external: NavBackStack<NavKey>) {

    FloatingActionButton(
        modifier = Modifier,
        onClick = {
            external.add(AddTransactionRoute)
        },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = "Añadir",
            modifier = Modifier.size(24.dp)
        )
    }
}