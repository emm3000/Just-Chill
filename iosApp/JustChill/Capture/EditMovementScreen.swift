@preconcurrency import JustChillKit
import SwiftUI

struct EditMovementScreen: View {
    typealias Store = MviStore<EditTransactionUiState, any EditTransactionIntent, any EditTransactionEffect>
    typealias Send = (any EditTransactionIntent) -> Void

    let transactionId: String
    let onClose: () -> Void

    @State private var store: Store?
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if let store {
                Content(state: store.state, send: { store.send($0) }, onClose: onClose)
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveEditTransactionHandle(transactionId: transactionId))
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .transactionUpdated, .transactionDeleted: onClose()
                case .showError(let failure): errorMessage = failure.message
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

extension EditMovementScreen {
    struct Content: View {
        let state: EditTransactionUiState
        let send: Send
        let onClose: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                TopBar(isSpend: isSpend, onClose: onClose) { send(EditTransactionIntentOnDeleteClick.shared) }
                CaptureScreen.SignToggle(isSpend: isSpend) {
                    send(EditTransactionIntentOnTransactionTypeChange(value: $0))
                }
                VStack(spacing: EmmSpacing.s0) {
                    CaptureScreen.Hero(amount: state.amount, isSpend: isSpend)
                        .frame(maxHeight: .infinity)
                    CaptureScreen.PadForm(
                        accountName: state.accountSelected?.name,
                        hasNoAccounts: false,
                        category: state.categorySelected,
                        dateLabel: state.dateLabel,
                        onAccount: { request(.account) },
                        onCategory: { request(.category) },
                        onDate: { request(.date) }
                    )
                    NoteRow(note: state.note) { request(.note) }
                }
                .frame(maxHeight: .infinity)
                Numpad(digits: amountBinding)
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.bottom, EmmSpacing.s2)
                CaptureScreen.SaveButton(
                    label: "Guardar cambios",
                    isEnabled: state.isEnabled,
                    isSaving: false,
                    onSave: { send(EditTransactionIntentOnSave.shared) }
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(EmmColors.bg)
            .sheet(isPresented: sheetBinding(.account)) { accountSheet }
            .sheet(isPresented: sheetBinding(.category)) { categorySheet }
            .sheet(isPresented: sheetBinding(.date)) { dateSheet }
            .sheet(isPresented: sheetBinding(.note)) {
                NoteSheet(
                    initialNote: state.note,
                    onSave: { send(EditTransactionIntentOnDescriptionChange(value: $0)) }, onDismiss: dismissSheet)
            }
            .sheet(isPresented: deleteBinding) {
                DeleteSheet(
                    isSpend: isSpend,
                    amount: state.amount,
                    category: state.categorySelected,
                    accountName: state.accountSelected?.name,
                    onConfirm: { send(EditTransactionIntentOnDeleteConfirm.shared) },
                    onDismiss: { send(EditTransactionIntentOnDeleteDismiss.shared) }
                )
            }
        }

        private var accountSheet: some View {
            CaptureScreen.AccountSheet(
                accounts: state.accounts,
                selected: state.accountSelected,
                onSelect: { send(EditTransactionIntentOnAccountSelected(value: $0)) },
                onDismiss: dismissSheet
            )
        }

        private var categorySheet: some View {
            CaptureScreen.CategorySheet(
                categories: state.categories,
                frequent: state.frequentCategories,
                other: state.otherCategories,
                selected: state.categorySelected,
                search: { state.categoriesMatching(query: $0) },
                onSelect: { send(EditTransactionIntentOnCategorySelected(value: $0)) },
                onDismiss: dismissSheet
            )
        }

        private var dateSheet: some View {
            CaptureScreen.DateSheet(
                pickedDay: state.date,
                today: state.today,
                shortcuts: state.dateShortcuts,
                onSelect: { send(EditTransactionIntentOnDateSelected(value: $0)) },
                onDismiss: dismissSheet
            )
        }

        private var isSpend: Bool {
            state.transactionType == TransactionType.spend
        }

        private var amountBinding: Binding<String> {
            Binding(
                get: { state.amount },
                set: { send(EditTransactionIntentOnAmountChange(value: $0)) }
            )
        }

        private var deleteBinding: Binding<Bool> {
            Binding(
                get: { state.showDeleteDialog },
                set: { isPresented in
                    if !isPresented, state.showDeleteDialog { send(EditTransactionIntentOnDeleteDismiss.shared) }
                }
            )
        }

        private func request(_ sheet: TransactionSheet) {
            send(EditTransactionIntentOnSheetRequested(sheet: sheet))
        }

        private func dismissSheet() {
            send(EditTransactionIntentOnSheetDismissed.shared)
        }

        private func sheetBinding(_ sheet: TransactionSheet) -> Binding<Bool> {
            Binding(
                get: { state.openSheet == sheet },
                set: { isPresented in
                    if !isPresented, state.openSheet == sheet { dismissSheet() }
                }
            )
        }
    }
}
