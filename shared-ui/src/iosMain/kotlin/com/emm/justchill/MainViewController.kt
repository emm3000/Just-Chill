package com.emm.justchill

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// iOS entry point consumed by ContentView.swift via MainViewControllerKt.MainViewController().
// 5b renders the full iOS nav host (IosApp) — bottom nav + local-first flows backed by the native
// SQLDelight driver and Koin. The theme is applied inside IosApp().
fun MainViewController(): UIViewController = ComposeUIViewController {
    IosApp()
}
