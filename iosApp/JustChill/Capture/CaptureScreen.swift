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

        @State private var isAddAccountPresented: Bool = false
        @State private var isAddCategoryRequested: Bool = false
        @State private var isAddCategoryPresented: Bool = false

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                TopBar(
                    label: state.monthSpendLabel, amount: state.monthSpendAmount, onClose: onClose,
                    onOpenMovements: onOpenMovements)
                SignToggle(isSpend: isSpend) { send(AddTransactionIntentOnTransactionTypeChange(value: $0)) }
                VStack(spacing: EmmSpacing.s0) {
                    Hero(amount: state.amount, isSpend: isSpend)
                        .frame(maxHeight: .infinity)
                    PadForm(
                        accountName: state.accountSelected?.name,
                        hasNoAccounts: state.hasNoAccounts,
                        category: state.categorySelected,
                        dateLabel: state.dateLabel,
                        onAccount: { request(.account) },
                        onCreateAccount: { isAddAccountPresented = true },
                        onCategory: { request(.category) },
                        onDate: { request(.date) }
                    )
                }
                .frame(maxHeight: .infinity)
                Numpad(digits: amountBinding)
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.bottom, EmmSpacing.s2)
                SaveButton(
                    label: saveLabel,
                    isEnabled: !state.isSaving && state.missingField == nil,
                    isSaving: state.isSaving,
                    onSave: { send(AddTransactionIntentOnSave.shared) }
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(EmmColors.bg)
            .sheet(isPresented: $isAddAccountPresented) {
                AddAccountScreen(onClose: { isAddAccountPresented = false })
            }
            .sheet(isPresented: sheetBinding(.account)) {
                AccountSheet(
                    accounts: state.accounts,
                    selected: state.accountSelected,
                    onSelect: { send(AddTransactionIntentOnAccountSelected(value: $0)) },
                    onDismiss: { send(AddTransactionIntentOnSheetDismissed.shared) }
                )
            }
            .sheet(isPresented: sheetBinding(.category), onDismiss: openRequestedAddCategory) {
                CategorySheet(
                    categories: state.categories,
                    frequent: state.frequentCategories,
                    other: state.otherCategories,
                    selected: state.categorySelected,
                    search: matches,
                    onSelect: { send(AddTransactionIntentOnCategorySelected(value: $0)) },
                    onAddNew: { isAddCategoryRequested = true },
                    onDismiss: { send(AddTransactionIntentOnSheetDismissed.shared) }
                )
            }
            .sheet(isPresented: $isAddCategoryPresented) {
                AddCategoryScreen(
                    initialType: state.transactionType.categoryType,
                    onClose: { isAddCategoryPresented = false },
                    onSaved: { created in
                        send(AddTransactionIntentOnNewValueFromOthers(value: created.toSelectable()))
                        isAddCategoryPresented = false
                    }
                )
            }
            .sheet(isPresented: sheetBinding(.date)) {
                DateSheet(
                    pickedDay: state.pickerDate,
                    today: state.today,
                    shortcuts: state.dateShortcuts,
                    onSelect: { send(AddTransactionIntentOnDateSelected(value: $0)) },
                    onDismiss: { send(AddTransactionIntentOnSheetDismissed.shared) }
                )
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

        private var saveLabel: String {
            if state.hasNoAccounts { return "Crea una cuenta primero" }
            return isSpend ? "Anotar gasto" : "Anotar ingreso"
        }

        private func openRequestedAddCategory() {
            guard isAddCategoryRequested else { return }
            isAddCategoryRequested = false
            isAddCategoryPresented = true
        }

        private func request(_ sheet: TransactionSheet) {
            send(AddTransactionIntentOnSheetRequested(sheet: sheet))
        }

        private func matches(_ query: String) -> [SelectableCategory] {
            state.categories.filter { $0.name.range(of: query, options: .caseInsensitive) != nil }
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
