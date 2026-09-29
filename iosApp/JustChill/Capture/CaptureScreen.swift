@preconcurrency import JustChillKit
import SwiftUI

struct CaptureScreen: View {
    typealias Store = MviStore<AddTransactionUiState, any AddTransactionIntent, any AddTransactionEffect>
    typealias Send = (any AddTransactionIntent) -> Void

    let onClose: () -> Void
    let onSaved: (YearMonth) -> Void
    let onOpenMovements: () -> Void

    @Environment(\.scenePhase) private var scenePhase: ScenePhase
    @State private var store: Store?
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if let store {
                Content(
                    state: store.state,
                    send: { store.send($0) },
                    onClose: onClose,
                    onOpenMovements: onOpenMovements
                )
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveAddTransactionHandle())
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .transactionSaved(let saved): onSaved(saved.month)
                case .showError(let failure): errorMessage = failure.message
                }
            }
            store = newStore
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                store?.send(AddTransactionIntentOnResumed.shared)
            }
        }
        .alert(errorMessage ?? "", isPresented: isErrorPresented) {
            Button("Aceptar") { errorMessage = nil }
        }
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

extension CaptureScreen {
    struct Content: View {
        let state: AddTransactionUiState
        let send: Send
        let onClose: () -> Void
        let onOpenMovements: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                TopBar(
                    label: state.monthSpendLabel, amount: state.monthSpendAmount, onClose: onClose,
                    onOpenMovements: onOpenMovements)
                SignToggle(isSpend: isSpend, send: send)
                VStack(spacing: EmmSpacing.s0) {
                    Hero(amount: state.amount, isSpend: isSpend)
                        .frame(maxHeight: .infinity)
                    PadForm(state: state, send: send)
                }
                .frame(maxHeight: .infinity)
                Numpad(digits: amountBinding)
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.bottom, EmmSpacing.s2)
                SaveButton(state: state, send: send)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(EmmColors.bg)
            .sheet(isPresented: sheetBinding(.account)) {
                AccountSheet(state: state, send: send)
            }
            .sheet(isPresented: sheetBinding(.category)) {
                CategorySheet(state: state, send: send)
            }
            .sheet(isPresented: sheetBinding(.date)) {
                DateSheet(state: state, send: send)
            }
        }

        private var isSpend: Bool {
            state.transactionType == TransactionType.spend
        }

        private var amountBinding: Binding<String> {
            Binding(
                get: { state.amount },
                set: { send(AddTransactionIntentOnAmountChange(value: $0)) }
            )
        }

        private func sheetBinding(_ sheet: TransactionSheet) -> Binding<Bool> {
            Binding(
                get: { state.openSheet == sheet },
                set: { isPresented in
                    if !isPresented, state.openSheet == sheet {
                        send(AddTransactionIntentOnSheetDismissed.shared)
                    }
                }
            )
        }
    }
}
