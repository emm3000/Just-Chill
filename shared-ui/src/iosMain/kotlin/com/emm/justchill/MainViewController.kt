package com.emm.justchill

import androidx.compose.ui.window.ComposeUIViewController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.seetransactions.SeeTransactionsScreen
import platform.UIKit.UIViewController

// iOS entry point consumed by ContentView.swift via MainViewControllerKt.MainViewController().
// 5a renders ONE real local-first screen (SeeTransactions) backed by the native
// SQLDelight driver. Editing navigation is a no-op stub — there is no iOS nav host yet.
// TODO 5b: build the iOS nav host (navigation3 CMP) and wire onEditTransaction.
fun MainViewController(): UIViewController = ComposeUIViewController {
    EmmTheme {
        SeeTransactionsScreen(
            onEditTransaction = { /* TODO 5b: navigate to edit on iOS */ },
        )
    }
}
