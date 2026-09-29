@preconcurrency import JustChillKit
import SwiftUI

@main
struct JustChillApp: App {
    @State private var isManifestoPending: Bool

    init() {
        EmmFonts.logAvailability()
        doInitKoin(config: KitConfig.offline(appVersion: Bundle.main.shortVersion))
        _isManifestoPending = State(initialValue: !resolveAppPreferences().firstLaunchSeen)
    }

    var body: some Scene {
        WindowGroup {
            if isManifestoPending {
                ManifestoScreen(isRevisit: false, onStart: markFirstLaunchSeen)
            } else {
                AppShell()
            }
        }
    }

    private func markFirstLaunchSeen() {
        resolveAppPreferences().firstLaunchSeen = true
        isManifestoPending = false
    }
}

private extension KitConfig {
    static func offline(appVersion: String) -> KitConfig {
        KitConfig(
            supabaseUrl: "",
            supabaseAnonKey: "",
            googleServerClientId: "",
            appVersion: appVersion,
            isSnapshotBackupEnabled: false
        )
    }
}

private extension Bundle {
    var shortVersion: String {
        object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
    }
}
