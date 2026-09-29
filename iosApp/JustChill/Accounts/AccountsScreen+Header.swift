@preconcurrency import JustChillKit
import SwiftUI

extension AccountsScreen {
    struct Header: View {
        let state: AccountsUiState
        let onAdd: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                HStack(spacing: EmmSpacing.s2) {
                    Text("Cuentas")
                        .emmTextStyle(EmmType.titleL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .accessibilityAddTraits(.isHeader)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s16, alignment: .leading)
                    newButton
                }
                .padding(.horizontal, EmmSpacing.s6)
                summary
                    .padding(.horizontal, EmmSpacing.s6)
                    .padding(.top, EmmSpacing.s2)
                    .padding(.bottom, EmmSpacing.s5)
                Hairline()
            }
        }

        private var newButton: some View {
            Button(action: onAdd) {
                HStack(spacing: EmmSpacing.s1) {
                    Image(systemName: "plus")
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                        .accessibilityHidden(true)
                    Text("Nueva")
                        .emmTextStyle(EmmType.labelL)
                        .lineLimit(1)
                }
                .foregroundStyle(EmmColors.textPrimary)
                .padding(.horizontal, EmmSpacing.s4)
                .frame(minHeight: EmmSpacing.s10)
                .background(EmmColors.surface1, in: EmmRadii.rFull)
                .overlay { EmmRadii.rFull.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .frame(minHeight: EmmSpacing.s12)
                .contentShape(Rectangle())
            }
            .accessibilityHint("Crear cuenta")
        }

        private var summary: some View {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: EmmSpacing.s6) {
                    spent
                    Rectangle()
                        .fill(EmmColors.border)
                        .frame(width: EmmSpacing.hairline)
                        .accessibilityHidden(true)
                    income
                    Spacer(minLength: EmmSpacing.s0)
                }
                .fixedSize(horizontal: false, vertical: true)
                VStack(alignment: .leading, spacing: EmmSpacing.s4) {
                    spent
                    income
                }
            }
        }

        private var spent: some View {
            column(
                eyebrow: "Gastado en \(state.month.monthLabel())", amount: state.monthSpent,
                color: EmmColors.textPrimary)
        }

        private var income: some View {
            column(eyebrow: "Ingresado", amount: state.monthIncome, color: EmmColors.textSecondary)
        }

        private func column(eyebrow: String, amount: String, color: Color) -> some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                Eyebrow(text: eyebrow)
                Text(amount)
                    .emmTextStyle(EmmType.amountLead)
                    .foregroundStyle(color)
                    .fixedSize(horizontal: true, vertical: false)
            }
            .accessibilityElement(children: .combine)
        }
    }
}
