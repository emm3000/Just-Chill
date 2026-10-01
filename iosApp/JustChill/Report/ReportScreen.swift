@preconcurrency import JustChillKit
import SwiftUI

struct ReportScreen: View {
    typealias Store = MviStore<ReportUiState, any ReportIntent, any ReportEffect>
    typealias Send = (any ReportIntent) -> Void

    @State private var store: Store?
    @State private var errorMessage: String?
    @State private var captureCover = CaptureCover()

    var body: some View {
        Group {
            if let store {
                Content(
                    state: store.state,
                    send: { store.send($0) },
                    onAddTransaction: { captureCover.present() }
                )
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveReportHandle())
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .showError(let failure): errorMessage = failure.message
                case .shareReport: break
                }
            }
            store = newStore
        }
        .onAppear { reloadVisibleTab() }
        .fullScreenCover(isPresented: isCapturePresented, onDismiss: finishLeavingCapture) {
            CaptureScreen(
                onClose: { captureCover.close() },
                onSaved: { month in
                    store?.send(ReportIntentSelectMonth(month: month))
                    captureCover.close()
                },
                onOpenMovements: { captureCover.close() }
            )
        }
        .alert(errorMessage ?? "", isPresented: isErrorPresented) {
            Button("Aceptar") { errorMessage = nil }
        }
    }

    private func reloadVisibleTab() {
        guard let store else { return }
        store.send(ReportIntentSelectTab(tab: store.state.selectedTab))
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

    private var isErrorPresented: Binding<Bool> {
        Binding(
            get: { errorMessage != nil },
            set: { isPresented in
                if !isPresented { errorMessage = nil }
            }
        )
    }
}

extension ReportScreen {
    struct Content: View {
        let state: ReportUiState
        let send: Send
        let onAddTransaction: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Text("Reporte")
                    .emmTextStyle(EmmType.titleL)
                    .foregroundStyle(EmmColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .accessibilityAddTraits(.isHeader)
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s16, alignment: .leading)
                    .padding(.horizontal, EmmSpacing.s4)
                Segmented(options: tabOptions)
                    .padding(.horizontal, EmmSpacing.s4)
                ScrollView {
                    VStack(alignment: .leading, spacing: EmmSpacing.s6) {
                        switch state.selectedTab {
                        case .month: MonthTab(state: state, send: send, onAddTransaction: onAddTransaction)
                        case .trends: TrendsTab(trends: state.trends)
                        }
                    }
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.vertical, EmmSpacing.s4)
                }
                .id(state.selectedTab)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .sheet(isPresented: isMonthSheetPresented) {
                MonthSheet(
                    current: state.month,
                    onSelect: { send(ReportIntentSelectMonth(month: $0)) },
                    onDismiss: { send(ReportIntentOnMonthSheetDismissed.shared) }
                )
            }
        }

        private var tabOptions: [Segmented.Option] {
            [
                Segmented.Option(
                    label: "Mes",
                    isSelected: state.selectedTab == .month,
                    select: { send(ReportIntentSelectTab(tab: .month)) }
                ),
                Segmented.Option(
                    label: "Tendencias \(ReportViewModelKt.TRENDS_WINDOW_MONTHS)m",
                    isSelected: state.selectedTab == .trends,
                    select: { send(ReportIntentSelectTab(tab: .trends)) }
                ),
            ]
        }

        private var isMonthSheetPresented: Binding<Bool> {
            Binding(
                get: { state.showMonthSheet },
                set: { isPresented in
                    if !isPresented { send(ReportIntentOnMonthSheetDismissed.shared) }
                }
            )
        }
    }

    struct Segmented: View {
        struct Option: Identifiable {
            let label: String
            let isSelected: Bool
            let select: () -> Void

            var id: String { label }
        }

        let options: [Option]

        var body: some View {
            HStack(spacing: EmmSpacing.s0) {
                ForEach(options) { option in
                    Button(action: option.select) {
                        Text(option.label)
                            .emmTextStyle(EmmType.labelL)
                            .foregroundStyle(option.isSelected ? EmmColors.textPrimary : EmmColors.textSecondary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.5)
                            .padding(.horizontal, EmmSpacing.s3)
                            .frame(maxWidth: .infinity, minHeight: EmmSpacing.s10)
                            .background(option.isSelected ? EmmColors.surface2 : Color.clear, in: EmmRadii.rS)
                            .padding(EmmSpacing.s1)
                            .contentShape(Rectangle())
                    }
                    .accessibilityAddTraits(option.isSelected ? .isSelected : [])
                }
            }
            .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
        }
    }

    struct HeaderRow<Trailing: View>: View {
        let title: String
        @ViewBuilder let trailing: Trailing

        var body: some View {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline) {
                    Eyebrow(text: title)
                    Spacer()
                    trailing
                }
                VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                    Eyebrow(text: title)
                    trailing
                }
            }
        }
    }

    struct Pill: View {
        let text: String
        let symbol: String?
        let isTinted: Bool

        var body: some View {
            HStack(spacing: EmmSpacing.s1) {
                if let symbol {
                    Image(systemName: symbol)
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                        .accessibilityHidden(true)
                }
                Text(text)
                    .emmTextStyle(EmmType.labelM)
                    .lineLimit(1)
                    .fixedSize(horizontal: true, vertical: false)
            }
            .foregroundStyle(isTinted ? EmmColors.success : EmmColors.textSecondary)
            .padding(.horizontal, EmmSpacing.s2)
            .padding(.vertical, EmmSpacing.s1)
            .overlay {
                EmmRadii.rFull.stroke(isTinted ? EmmColors.success : EmmColors.border, lineWidth: EmmSpacing.hairline)
            }
        }
    }
}
