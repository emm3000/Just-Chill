@preconcurrency import JustChillKit
import SwiftUI

extension CaptureScreen {
    struct TopBar: View {
        let label: String
        let amount: String
        let onClose: () -> Void
        let onOpenMovements: () -> Void

        var body: some View {
            HStack(alignment: .top, spacing: EmmSpacing.s0) {
                Button(action: onClose) {
                    Image(systemName: "xmark")
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                        .foregroundStyle(EmmColors.textSecondary)
                        .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                }
                .accessibilityLabel("Cerrar")
                Button(action: onOpenMovements) {
                    HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                        ViewThatFits(in: .horizontal) {
                            HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                                eyebrow.lineLimit(1)
                                total
                            }
                            VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                                eyebrow.fixedSize(horizontal: false, vertical: true)
                                total
                            }
                        }
                        Spacer(minLength: EmmSpacing.s0)
                        Image(systemName: "chevron.right")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                            .foregroundStyle(EmmColors.textTertiary)
                    }
                    .padding(.horizontal, EmmSpacing.s4)
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label + ": " + amount + ". Toca para ver tus movimientos.")
                .accessibilityAddTraits(.isButton)
            }
            .padding(.leading, EmmSpacing.s4)
        }

        private var eyebrow: some View {
            Text(label.uppercased())
                .emmTextStyle(EmmType.eyebrow)
                .foregroundStyle(EmmColors.textTertiary)
                .multilineTextAlignment(.leading)
        }

        private var total: some View {
            Text(amount)
                .emmTextStyle(EmmType.amountM)
                .foregroundStyle(EmmColors.textSecondary)
                .lineLimit(1)
        }
    }

    struct SignToggle: View {
        let isSpend: Bool
        let send: Send

        var body: some View {
            HStack(spacing: EmmSpacing.s0) {
                segment(label: "Ingreso", isSelected: !isSpend, type: TransactionType.income)
                segment(label: "Gasto", isSelected: isSpend, type: TransactionType.spend)
            }
            .fixedSize()
            .overlay { EmmRadii.rFull.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, EmmSpacing.s4)
        }

        private func segment(label: String, isSelected: Bool, type: TransactionType) -> some View {
            Button {
                send(AddTransactionIntentOnTransactionTypeChange(value: type))
            } label: {
                Text(label)
                    .emmTextStyle(EmmType.labelL)
                    .foregroundStyle(isSelected ? EmmColors.textPrimary : EmmColors.textTertiary)
                    .lineLimit(1)
                    .padding(.horizontal, EmmSpacing.s3)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(isSelected ? EmmColors.surface2 : Color.clear, in: EmmRadii.rFull)
                    .padding(EmmSpacing.s1)
                    .frame(minWidth: EmmSpacing.s16 + EmmSpacing.s8, minHeight: EmmSpacing.s12)
                    .contentShape(Rectangle())
            }
            .accessibilityAddTraits(isSelected ? .isSelected : [])
        }
    }

    struct Hero: View {
        let amount: String
        let isSpend: Bool

        var body: some View {
            HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                Text("S/")
                    .emmTextStyle(EmmType.amountLead)
                    .foregroundStyle(EmmColors.textTertiary)
                Text(CentsFormatterKt.formatCentsForDisplay(digits: amount))
                    .emmTextStyle(EmmType.amountHero)
                    .foregroundStyle(isSpend ? EmmColors.textPrimary : EmmColors.success)
                    .lineLimit(1)
                    .minimumScaleFactor(0.25)
            }
            .padding(.horizontal, EmmSpacing.s6)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(description)
        }

        private var description: String {
            let cents: Int64 = CentsFormatterKt.centsToMoney(digits: amount)
            return isSpend
                ? "Gasto de " + CurrencyFormatKt.balanceFormatted(cents)
                : "Ingreso de " + CurrencyFormatKt.positiveMoneyFormatted(cents)
        }
    }

    struct PadForm: View {
        let state: AddTransactionUiState
        let send: Send

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: EmmSpacing.s2) { chips }
                    VStack(alignment: .leading, spacing: EmmSpacing.s0) { chips }
                }
                .padding(.horizontal, EmmSpacing.s4)
                Button {
                    send(AddTransactionIntentOnSheetRequested(sheet: TransactionSheet.date))
                } label: {
                    HStack(spacing: EmmSpacing.s1) {
                        Text(state.dateLabel)
                            .emmTextStyle(EmmType.labelL)
                            .foregroundStyle(EmmColors.textPrimary)
                        Image(systemName: "chevron.down")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                            .foregroundStyle(EmmColors.textTertiary)
                    }
                    .frame(minWidth: EmmSpacing.s12, minHeight: EmmSpacing.s12)
                    .contentShape(Rectangle())
                }
                .accessibilityHint("Cambiar la fecha")
                .padding(.horizontal, EmmSpacing.s6)
            }
        }

        private var chips: some View {
            Group {
                accountChip
                    .fixedSize(horizontal: true, vertical: false)
                SelectorChip(
                    label: state.categorySelected?.name ?? "—",
                    dotColor: state.categorySelected.map { EmmCategory.resolvedColor($0.colorId) },
                    hint: "Cambiar la categoría"
                ) {
                    send(AddTransactionIntentOnSheetRequested(sheet: TransactionSheet.category))
                }
            }
        }

        @ViewBuilder
        private var accountChip: some View {
            if state.hasNoAccounts {
                SelectorChip(label: "Crear cuenta", dotColor: nil, symbol: "plus", hint: "Crear una cuenta") {}
                    .disabled(true)
            } else {
                SelectorChip(label: state.accountSelected?.name ?? "—", dotColor: nil, hint: "Cambiar la cuenta") {
                    send(AddTransactionIntentOnSheetRequested(sheet: TransactionSheet.account))
                }
            }
        }
    }

    struct SelectorChip: View {
        let label: String
        let dotColor: Color?
        var symbol: String = "chevron.down"
        let hint: String
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                HStack(spacing: EmmSpacing.s2) {
                    if let dotColor {
                        Circle()
                            .fill(dotColor)
                            .frame(width: EmmSpacing.s2, height: EmmSpacing.s2)
                    }
                    Text(label)
                        .emmTextStyle(EmmType.labelL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Image(systemName: symbol)
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                        .foregroundStyle(EmmColors.textTertiary)
                }
                .padding(.horizontal, EmmSpacing.s3)
                .frame(minHeight: EmmSpacing.s10)
                .overlay { EmmRadii.rFull.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .frame(minHeight: EmmSpacing.s12)
                .contentShape(Rectangle())
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityHint(hint)
            .accessibilityAddTraits(.isButton)
        }
    }

    struct SaveButton: View {
        let state: AddTransactionUiState
        let send: Send

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Rectangle()
                    .fill(EmmColors.border)
                    .frame(height: EmmSpacing.hairline)
                Button {
                    send(AddTransactionIntentOnSave.shared)
                } label: {
                    HStack(spacing: EmmSpacing.s2) {
                        if state.isSaving {
                            ProgressView()
                                .tint(EmmColors.textTertiary)
                        }
                        Text(label)
                            .emmTextStyle(EmmType.titleM)
                    }
                    .foregroundStyle(isEnabled ? EmmColors.bg : EmmColors.textTertiary)
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
                    .background(isEnabled ? EmmColors.textPrimary : EmmColors.surface1, in: EmmRadii.rL)
                }
                .disabled(!isEnabled)
                .padding(.horizontal, EmmSpacing.s4)
                .padding(.top, EmmSpacing.s3)
                .padding(.bottom, EmmSpacing.s4)
            }
        }

        private var isEnabled: Bool {
            !state.isSaving && state.missingField == nil
        }

        private var label: String {
            if state.hasNoAccounts { return "Crea una cuenta primero" }
            return state.transactionType == TransactionType.spend ? "Anotar gasto" : "Anotar ingreso"
        }
    }
}
