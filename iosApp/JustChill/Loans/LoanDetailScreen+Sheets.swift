@preconcurrency import JustChillKit
import SwiftUI

extension LoanDetailScreen {
    struct PaymentFormSheet: View {
        let form: LoanPaymentFormUi
        let send: Send

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                SheetTitleBar(title: title, onClose: { send(LoanDetailIntentPaymentFormIntentOnPaymentDismiss.shared) })
                ScrollView {
                    VStack(spacing: EmmSpacing.s5) {
                        amountSection
                        LoansScreen.FormSection(eyebrow: "MÉTODO") { methods }
                        LoansScreen.FormSection(eyebrow: "FECHA") {
                            LoansScreen.DateRow(label: form.dateLabel) { request(.date) }
                        }
                        LoansScreen.FormSection(eyebrow: "NOTA · OPCIONAL") {
                            LoansScreen.TextEntry(placeholder: "Ej. Pago en efectivo", text: noteBinding)
                        }
                    }
                    .padding(.horizontal, EmmSpacing.s5)
                    .padding(.vertical, EmmSpacing.s4)
                }
                .scrollDismissesKeyboard(.interactively)
                SaveButton(
                    label: title,
                    isEnabled: form.isSaveEnabled && !form.isSaving,
                    isSaving: form.isSaving,
                    onSave: { send(LoanDetailIntentPaymentFormIntentOnPaymentConfirm.shared) }
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents([.large])
            .sheet(isPresented: sheetBinding(.amount)) { amountSheet }
            .sheet(isPresented: sheetBinding(.date)) { dateSheet }
        }

        private var title: String {
            form.editingPaymentId == nil ? "Registrar abono" : "Editar abono"
        }

        private var amountSection: some View {
            LoansScreen.FormSection(eyebrow: "MONTO") {
                LoansScreen.AmountCard(amountDigits: form.amountDigits) { request(.amount) }
                if let message = form.amountError {
                    Text(message)
                        .emmTextStyle(EmmType.labelM)
                        .foregroundStyle(EmmColors.danger)
                }
            }
        }

        private var methods: some View {
            HStack(spacing: EmmSpacing.s0) {
                ForEach(form.methodOptions, id: \.method) { option in
                    Button {
                        send(LoanDetailIntentPaymentFormIntentOnPaymentMethodChange(method: option.method))
                    } label: {
                        Text(option.label)
                            .emmTextStyle(EmmType.labelL)
                            .foregroundStyle(option.isSelected ? EmmColors.bg : EmmColors.textSecondary)
                            .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12)
                            .background(option.isSelected ? EmmColors.textPrimary : Color.clear, in: EmmRadii.rM)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(option.isSelected ? .isSelected : [])
                }
            }
            .background(EmmColors.surface1, in: EmmRadii.rM)
            .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
        }

        private var amountSheet: some View {
            LoansScreen.AmountSheet(
                title: "Monto del abono",
                subtitle: form.maxAmountLabel.map { "Máximo \($0)" },
                amountDigits: form.amountDigits,
                onConfirm: { send(LoanDetailIntentPaymentFormIntentOnPaymentAmountChange(digits: $0)) },
                onDismiss: dismissSheet
            )
        }

        private var dateSheet: some View {
            DateSheet(
                pickedDay: form.pickerDate,
                today: form.today,
                shortcuts: form.dateShortcuts,
                onSelect: { send(LoanDetailIntentPaymentFormIntentOnPaymentDateSelected(value: $0)) },
                onDismiss: dismissSheet
            )
        }

        private var noteBinding: Binding<String> {
            Binding(
                get: { form.note },
                set: { send(LoanDetailIntentPaymentFormIntentOnPaymentNoteChange(value: $0)) }
            )
        }

        private func request(_ sheet: PaymentSheet) {
            send(LoanDetailIntentPaymentFormIntentOnPaymentSheetRequested(sheet: sheet))
        }

        private func dismissSheet() {
            send(LoanDetailIntentPaymentFormIntentOnPaymentSheetDismissed.shared)
        }

        private func sheetBinding(_ sheet: PaymentSheet) -> Binding<Bool> {
            Binding(
                get: { form.openSheet == sheet },
                set: { isPresented in
                    if !isPresented, form.openSheet == sheet { dismissSheet() }
                }
            )
        }
    }

    struct DeleteSheet: View {
        let title: String
        let message: String
        let isDeleting: Bool
        let onConfirm: () -> Void
        let onDismiss: () -> Void
        @State private var contentHeight: CGFloat = EmmSpacing.s0

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s3) {
                Text(title)
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Text(message)
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                Button(action: onConfirm) {
                    HStack(spacing: EmmSpacing.s2) {
                        if isDeleting {
                            ProgressView()
                                .tint(EmmColors.danger)
                        }
                        Text("Borrar")
                            .emmTextStyle(EmmType.titleM)
                    }
                    .foregroundStyle(EmmColors.danger)
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
                    .overlay { EmmRadii.rL.stroke(EmmColors.danger, lineWidth: EmmSpacing.hairline) }
                }
                .disabled(isDeleting)
                .padding(.top, EmmSpacing.s3)
                Button(action: onDismiss) {
                    Text("Cancelar")
                        .emmTextStyle(EmmType.titleM)
                        .foregroundStyle(EmmColors.textSecondary)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12)
                }
            }
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.top, EmmSpacing.s6)
            .padding(.bottom, EmmSpacing.s4)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .onGeometryChange(for: CGFloat.self) {
                $0.size.height
            } action: {
                contentHeight = $0
            }
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents(contentHeight > EmmSpacing.s0 ? [.height(contentHeight)] : [.medium])
        }
    }
}
