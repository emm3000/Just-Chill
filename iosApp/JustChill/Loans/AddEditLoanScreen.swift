@preconcurrency import JustChillKit
import SwiftUI

struct AddEditLoanScreen: View {
    typealias Store = MviStore<AddEditLoanUiState, any AddEditLoanIntent, any AddEditLoanEffect>
    typealias Send = (any AddEditLoanIntent) -> Void

    let loanId: String?
    let onClose: () -> Void

    @State private var store: Store?
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if let store {
                Content(
                    state: store.state, send: { store.send($0) },
                    currentInterest: { store.state.interestPercentText }, onClose: onClose)
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveAddEditLoanHandle(loanId: loanId))
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .navigateBack: onClose()
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

extension AddEditLoanScreen {
    struct Content: View {
        let state: AddEditLoanUiState
        let send: Send
        let currentInterest: @MainActor () -> String
        let onClose: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                TopBar(title: state.isEdit ? "Editar préstamo" : "Nuevo préstamo", onBack: onClose)
                ScrollView {
                    VStack(spacing: EmmSpacing.s5) {
                        personSection
                        LoansScreen.FormSection(eyebrow: "MONTO") {
                            VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                                LoansScreen.AmountCard(amountDigits: state.amountDigits) { request(.amount) }
                                FieldError(message: state.amountError)
                            }
                        }
                        LoansScreen.FormSection(eyebrow: "INTERÉS %") {
                            VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                                LoansScreen.SanitizedEntry(
                                    placeholder: "0", text: state.interestPercentText, current: currentInterest,
                                    keyboard: .decimalPad, isError: state.interestError != nil
                                ) { send(AddEditLoanIntentOnInterestPercentChange(value: $0)) }
                                FieldError(message: state.interestError)
                            }
                        }
                        LoansScreen.FormSection(eyebrow: "FECHA") {
                            LoansScreen.DateRow(label: state.dateLabel) { request(.date) }
                        }
                        LoansScreen.FormSection(eyebrow: "NOTA · OPCIONAL") {
                            LoansScreen.TextEntry(placeholder: "Ej. Prestado en efectivo", text: noteBinding)
                        }
                    }
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.vertical, EmmSpacing.s4)
                }
                .scrollDismissesKeyboard(.interactively)
                SaveButton(
                    label: state.isEdit ? "Guardar cambios" : "Crear préstamo",
                    isEnabled: state.isSaveEnabled && !state.isSaving,
                    isSaving: state.isSaving,
                    onSave: { send(AddEditLoanIntentSave.shared) }
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .sheet(isPresented: sheetBinding(.amount)) { amountSheet }
            .sheet(isPresented: sheetBinding(.date)) { dateSheet }
        }

        private var personSection: some View {
            LoansScreen.FormSection(eyebrow: "PERSONA") {
                LoansScreen.TextEntry(placeholder: "Ej. Juan", text: personBinding, isAutocorrectionDisabled: true)
                if !state.personSuggestions.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: EmmSpacing.s2) {
                            ForEach(state.personSuggestions, id: \.self) { name in
                                ShortcutPill(label: name, isActive: false) {
                                    send(AddEditLoanIntentOnPersonNameChange(value: name))
                                }
                            }
                        }
                    }
                }
            }
        }

        private var amountSheet: some View {
            LoansScreen.AmountSheet(
                title: "Monto del préstamo",
                subtitle: nil,
                amountDigits: state.amountDigits,
                onConfirm: { send(AddEditLoanIntentOnAmountChange(digits: $0)) },
                onDismiss: dismissSheet
            )
        }

        private var dateSheet: some View {
            DateSheet(
                pickedDay: state.pickerDate,
                today: state.today,
                shortcuts: state.dateShortcuts,
                onSelect: { send(AddEditLoanIntentOnDateSelected(value: $0)) },
                onDismiss: dismissSheet
            )
        }

        private var personBinding: Binding<String> {
            Binding(
                get: { state.personName },
                set: { send(AddEditLoanIntentOnPersonNameChange(value: $0)) }
            )
        }

        private var noteBinding: Binding<String> {
            Binding(
                get: { state.note },
                set: { send(AddEditLoanIntentOnNoteChange(value: $0)) }
            )
        }

        private func request(_ sheet: LoanFormSheet) {
            send(AddEditLoanIntentOnSheetRequested(sheet: sheet))
        }

        private func dismissSheet() {
            send(AddEditLoanIntentOnSheetDismissed.shared)
        }

        private func sheetBinding(_ sheet: LoanFormSheet) -> Binding<Bool> {
            Binding(
                get: { state.openSheet == sheet },
                set: { isPresented in
                    if !isPresented, state.openSheet == sheet { dismissSheet() }
                }
            )
        }
    }
}
