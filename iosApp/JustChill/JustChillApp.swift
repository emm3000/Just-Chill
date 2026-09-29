@preconcurrency import JustChillKit
import SwiftUI

@main
struct JustChillApp: App {
    init() {
        doInitKoin(config: KitConfig.offline(appVersion: Bundle.main.shortVersion))
    }

    var body: some Scene {
        WindowGroup {
            AppShell()
        }
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
