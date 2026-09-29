@preconcurrency import JustChillKit
import SwiftUI

struct MovementsScreen: View {
    typealias Store = MviStore<SeeTransactionsUiState, any SeeTransactionsIntent, any SeeTransactionsEffect>
    typealias Send = (any SeeTransactionsIntent) -> Void

    @State private var store: Store?
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if let store {
                    Content(state: store.state, send: { store.send($0) })
                } else {
                    EmmColors.bg
                }
            }
            .background(EmmColors.bg)
            .toolbar(.hidden, for: .navigationBar)
        }
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveSeeTransactionsHandle())
            newStore.onEffect { effect in
                if let failure = effect as? SeeTransactionsEffectShowError {
                    errorMessage = failure.message
                }
            }
            store = newStore
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

extension MovementsScreen {
    struct Content: View {
        let state: SeeTransactionsUiState
        let send: Send

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                if state.isSearchOpen {
                    SearchBar(query: state.query, send: send)
                } else {
                    Header(isFilterActive: state.isCategoryOrAmountFilterActive, send: send)
                }
                if state.isEyebrowVisible {
                    MonthHeader(state: state, send: send)
                }
                if state.isCategoryOrAmountFilterActive {
                    FilterBanner(state: state, send: send)
                }
                DayList(state: state, send: send)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .sheet(isPresented: isFilterSheetPresented) {
                FilterSheet(state: state, send: send)
            }
            .sheet(isPresented: isMonthPickerPresented) {
                MonthPickerSheet(current: state.month, send: send)
            }
        }

        private var isFilterSheetPresented: Binding<Bool> {
            Binding(
                get: { state.showFilterSheet },
                set: { isPresented in
                    if !isPresented { send(SeeTransactionsIntentScreenChromeIntentOnFilterSheetDismissed.shared) }
                }
            )
        }

        private var isMonthPickerPresented: Binding<Bool> {
            Binding(
                get: { state.showMonthPicker },
                set: { isPresented in
                    if !isPresented { send(SeeTransactionsIntentScreenChromeIntentOnMonthPickerDismissed.shared) }
                }
            )
        }
    }
}
