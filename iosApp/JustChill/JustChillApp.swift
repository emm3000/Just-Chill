@preconcurrency import JustChillKit
import SwiftUI
import UIKit

@main
struct JustChillApp: App {
    @State private var isManifestoPending: Bool

    init() {
        EmmFonts.logAvailability()
        let tabBarAppearance = UITabBarAppearance.warningBadges()
        UITabBar.appearance().standardAppearance = tabBarAppearance
        UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
        doInitKoin(config: KitConfig.bundled(appVersion: Bundle.main.shortVersion))
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
    static func bundled(appVersion: String) -> KitConfig {
        let url = Bundle.main.infoString(forKey: "SupabaseUrl")
        let anonKey = Bundle.main.infoString(forKey: "SupabaseAnonKey")
        return KitConfig(
            supabaseUrl: url,
            supabaseAnonKey: anonKey,
            googleServerClientId: "",
            appVersion: appVersion,
            isSnapshotBackupEnabled: !url.isEmpty && !anonKey.isEmpty
        )
    }
}

private extension UITabBarAppearance {
    static func warningBadges() -> UITabBarAppearance {
        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        for layout in [
            appearance.stackedLayoutAppearance, appearance.inlineLayoutAppearance,
            appearance.compactInlineLayoutAppearance,
        ] {
            layout.normal.badgeBackgroundColor = UIColor(EmmColors.warning)
            layout.selected.badgeBackgroundColor = UIColor(EmmColors.warning)
        }
        return appearance
    }
}

private extension Bundle {
    var shortVersion: String {
        infoString(forKey: "CFBundleShortVersionString")
    }

    func infoString(forKey key: String) -> String {
        object(forInfoDictionaryKey: key) as? String ?? ""
    }
}
