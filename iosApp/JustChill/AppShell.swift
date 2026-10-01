@preconcurrency import JustChillKit
import SwiftUI

enum AppTab: Hashable {
    case movements
    case report
    case capture
    case accounts
    case more
}

struct CaptureCover {
    private enum Phase {
        case hidden
        case shown
        case leaving
    }

    private var phase: Phase = .hidden

    var isShown: Bool { phase == .shown }

    // The tab bar takes taps while the cover is still leaving; presenting then reopens a fresh pad under a double
    // tap on its save (#639). Only .hidden, which the cover's onDismiss sets, may present.
    mutating func present() {
        guard phase == .hidden else { return }
        phase = .shown
    }

    mutating func close() {
        guard phase == .shown else { return }
        phase = .leaving
    }

    mutating func finishLeaving() {
        phase = .hidden
    }
}

struct AppShell: View {
    @State private var selectedTab: AppTab = .movements
    @State private var captureCover = CaptureCover()
    @State private var savedMonth: YearMonth?
    @State private var disclosureWatch: BackupDisclosureWatch?
    @State private var isDisclosurePending: Bool = false

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
                .tabItem {
                    Label {
                        Text("Más").accessibilityLabel(isDisclosurePending ? "Más, requiere tu atención" : "Más")
                    } icon: {
                        Image(systemName: "ellipsis")
                    }
                }
                .badge(isDisclosurePending ? Text("") : nil)
                .tag(AppTab.more)
        }
        .task { watchDisclosure() }
        .onDisappear(perform: stopWatchingDisclosure)
        .fullScreenCover(isPresented: isCapturePresented, onDismiss: finishLeavingCapture) {
            CaptureScreen(
                onClose: { captureCover.close() },
                onSaved: { month in
                    savedMonth = month
                    showMovements()
                },
                onOpenMovements: showMovements
            )
        }
    }

    private func watchDisclosure() {
        guard disclosureWatch == nil else { return }
        let watch = resolveBackupDisclosureWatch()
        watch.start { pending in
            MainActor.assumeIsolated { isDisclosurePending = pending.boolValue }
        }
        disclosureWatch = watch
    }

    private func stopWatchingDisclosure() {
        disclosureWatch?.stop()
        disclosureWatch = nil
    }

    private func showMovements() {
        selectedTab = .movements
        captureCover.close()
    }

    private func finishLeavingCapture() {
        captureCover.finishLeaving()
    }

    private var isCapturePresented: Binding<Bool> {
        Binding(
            get: { captureCover.isShown },
            set: { isShown in isShown ? captureCover.present() : captureCover.close() }
        )
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
                    captureCover.present()
                    Task { selectedTab = current }
                } else {
                    selectedTab = tab
                }
            }
        )
    }
}
