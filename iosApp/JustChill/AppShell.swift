import SwiftUI

enum AppTab: Hashable {
    case movements
    case report
    case capture
    case accounts
    case more
}

struct AppShell: View {
    @State private var selectedTab: AppTab = .movements
    @State private var isCapturePresented: Bool = false

    var body: some View {
        TabView(selection: tabSelection) {
            MovementsScreen()
                .tabItem { Label("Movimientos", systemImage: "list.bullet.rectangle") }
                .tag(AppTab.movements)
            ReportScreen()
                .tabItem { Label("Reporte", systemImage: "chart.bar.xaxis") }
                .tag(AppTab.report)
            Color.clear
                .tabItem {
                    Label {
                        Text("Anotar").accessibilityLabel("Anotar movimiento")
                    } icon: {
                        Image(systemName: "plus.circle.fill")
                    }
                }
                .tag(AppTab.capture)
            AccountsScreen()
                .tabItem { Label("Cuentas", systemImage: "wallet.bifold") }
                .tag(AppTab.accounts)
            MoreScreen()
                .tabItem { Label("Más", systemImage: "ellipsis") }
                .tag(AppTab.more)
        }
        .fullScreenCover(isPresented: $isCapturePresented) {
            CaptureScreen(onClose: { isCapturePresented = false })
        }
    }

    private var tabSelection: Binding<AppTab> {
        Binding(
            get: { selectedTab },
            set: { tab in
                if tab == .capture {
                    isCapturePresented = true
                } else {
                    selectedTab = tab
                }
            }
        )
    }
}
