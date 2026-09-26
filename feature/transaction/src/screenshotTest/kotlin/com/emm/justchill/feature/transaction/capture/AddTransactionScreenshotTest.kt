package com.emm.justchill.feature.transaction.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.emm.justchill.core.ui.preview.PreviewWindowEdges
import com.emm.justchill.core.ui.theme.EmmTheme

@PreviewTest
@PreviewWindowEdges
@Composable
internal fun AddTransactionPopulatedScreenshot() {
    EmmTheme {
        AddTransactionScreenContent(
            state = populatedCaptureState(),
            onIntent = {},
            onClose = {},
            onOpenTransactions = {},
            onSave = {},
        )
    }
}

@PreviewTest
@Preview(name = "360x640", device = "spec:width=360dp,height=640dp,dpi=320")
@Composable
internal fun AddTransactionNoAccountsScreenshot() {
    EmmTheme {
        AddTransactionScreenContent(
            state = noAccountsCaptureState(),
            onIntent = {},
            onClose = {},
            onOpenTransactions = {},
            onSave = {},
        )
    }
}
