@preconcurrency import JustChillKit
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
    @State private var savedMonth: YearMonth?

    var body: some View {
        TabView(selection: tabSelection) {
            MovementsScreen(savedMonth: savedMonth, onSavedMonthApplied: { savedMonth = nil })
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
            CaptureScreen(
                onClose: { isCapturePresented = false },
                onSaved: { month in
                    savedMonth = month
                    showMovements()
                },
                onOpenMovements: showMovements
            )
        }
    }

    private func showMovements() {
        selectedTab = .movements
        isCapturePresented = false
    }

    private var tabSelection: Binding<AppTab> {
        Binding(
            get: { selectedTab },
            set: { tab in
                if tab == .capture {
                    // TabView keeps a refused tab on screen until the bound value changes; bouncing through
                    // .capture and back resyncs it, or the blank add tab shows once the pad closes.
                    let current: AppTab = selectedTab
                    selectedTab = .capture
                    isCapturePresented = true
                    Task { selectedTab = current }
                } else {
                    selectedTab = tab
                }
            }
        )
    }
}
