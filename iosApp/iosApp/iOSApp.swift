import SwiftUI
import JustChillKit

@main
struct iOSApp: App {
    init() {
        // Start Koin once, before any view resolves a ViewModel.
        // `initKoin` is exported to Swift as `doInitKoin()` (Kotlin/Native mangles
        // the `init` prefix to avoid clashing with Obj-C init conventions).
        KoinIosKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}