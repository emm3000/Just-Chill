@preconcurrency import JustChillKit
import SwiftUI

extension PersonLoansScreen {
    struct LoanRow: View {
        let loan: LoanRowUi
        let onOpen: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Button(action: onOpen) { layout }
                    .buttonStyle(.plain)
                    .accessibilityHint("Ver detalle del préstamo")
                Hairline()
            }
        }

        private var layout: some View {
            HStack(spacing: EmmSpacing.s3) {
                VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                    HStack(spacing: EmmSpacing.s2) {
                        Text(loan.readableLentAt)
                            .emmTextStyle(EmmType.caption)
                            .foregroundStyle(EmmColors.textTertiary)
                        if loan.isSettled {
                            LoansScreen.SettledPill()
                        }
                    }
                    Text(loan.remaining)
                        .emmTextStyle(EmmType.amountLead)
                        .foregroundStyle(loan.isSettled ? EmmColors.textTertiary : EmmColors.textPrimary)
                    Text("Prestado \(loan.principal) · Total \(loan.totalDue) · Pagado \(loan.paidSoFar)")
                        .emmTextStyle(EmmType.caption)
                        .foregroundStyle(EmmColors.textSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, EmmSpacing.s5)
            .padding(.vertical, EmmSpacing.s3)
            .contentShape(Rectangle())
            .accessibilityElement(children: .combine)
        }
    }

    struct EmptyState: View {
        let personName: String

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Image(systemName: "banknote")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s10, height: EmmSpacing.s10)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
                Text("Sin préstamos con \(personName)")
                    .emmTextStyle(EmmType.titleL)
                    .foregroundStyle(EmmColors.textPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.top, EmmSpacing.s4)
            }
            .padding(.horizontal, EmmSpacing.s6)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
