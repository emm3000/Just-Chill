package com.emm.justchill

import androidx.compose.ui.window.ComposeUIViewController
import com.emm.justchill.hh.shared.AppNavHost
import platform.UIKit.UIViewController

// iOS entry point consumed by ContentView.swift via MainViewControllerKt.MainViewController().
// Renders the unified commonMain nav host (AppNavHost) — bottom nav + local-first flows backed by the
// native SQLDelight driver and Koin. The theme is applied inside AppNavHost().
fun MainViewController(): UIViewController = ComposeUIViewController {
    AppNavHost()
}
