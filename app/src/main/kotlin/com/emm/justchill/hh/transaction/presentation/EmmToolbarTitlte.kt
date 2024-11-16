package com.emm.justchill.hh.transaction.presentation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmmToolbarTitle(
    title: String,
    navigationIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = LatoFontFamily,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = dropUnlessResumed {
                navigationIconClick()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmmCenteredToolbar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {

    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = LatoFontFamily,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        actions = actions
    )
}

@PreviewLightDark
@Composable
private fun EmmCenteredToolbarPreview() {
    EmmTheme {
        EmmCenteredToolbar(
            title = "Transacciones",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@PreviewLightDark
@Composable
private fun EmmToolbarTitlePreview() {
    EmmTheme {
        EmmToolbarTitle(
            title = "Add transaction",
            navigationIconClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}