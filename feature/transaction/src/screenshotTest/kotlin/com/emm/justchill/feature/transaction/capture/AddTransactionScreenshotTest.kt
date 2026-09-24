package com.emm.justchill.feature.transaction.capture

import androidx.compose.runtime.Composable
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
            onOpenMenu = {},
            onOpenTransactions = {},
            onSave = {},
        )
    }
}
