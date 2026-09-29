@preconcurrency import JustChillKit
import SwiftUI

extension LoanDetailScreen {
    struct TopBar: View {
        let title: String
        let onBack: () -> Void
        let onEdit: () -> Void
        let onDelete: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                HStack(spacing: EmmSpacing.s2) {
                    barButton(symbol: "chevron.left", color: EmmColors.textPrimary, label: "Volver", action: onBack)
                    Text(title)
                        .emmTextStyle(EmmType.titleL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .accessibilityAddTraits(.isHeader)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s16, alignment: .leading)
                    barButton(symbol: "pencil", color: EmmColors.textPrimary, label: "Editar préstamo", action: onEdit)
                    barButton(symbol: "trash", color: EmmColors.danger, label: "Eliminar préstamo", action: onDelete)
                }
                .padding(.horizontal, EmmSpacing.s2)
                Hairline()
            }
        }

        private func barButton(symbol: String, color: Color, label: String, action: @escaping () -> Void)
            -> some View
        {
            Button(action: action) {
                Image(systemName: symbol)
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                    .foregroundStyle(color)
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(label)
        }
    }

    struct SummaryCard: View {
        let summary: LoanSummaryUi

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                HStack(spacing: EmmSpacing.s2) {
                    Text("Prestado el \(summary.readableLentAt)")
                        .emmTextStyle(EmmType.caption)
                        .foregroundStyle(EmmColors.textTertiary)
                    if summary.isSettled {
                        LoansScreen.SettledPill()
                    }
                }
                Text(summary.remaining)
                    .emmTextStyle(EmmType.amountCard)
                    .foregroundStyle(summary.isSettled ? EmmColors.textTertiary : EmmColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .padding(.top, EmmSpacing.s2)
                Text("Por cobrar")
                    .emmTextStyle(EmmType.caption)
                    .foregroundStyle(EmmColors.textSecondary)
                Hairline()
                    .padding(.vertical, EmmSpacing.s3)
                statRow(label: "Prestado", value: summary.principal)
                statRow(label: "Interés", value: summary.interestPercentLabel)
                statRow(label: "Total a pagar", value: summary.totalDue)
                statRow(label: "Pagado", value: summary.paidSoFar)
                if !summary.note.isEmpty {
                    Text(summary.note)
                        .emmTextStyle(EmmType.bodyM)
                        .italic()
                        .foregroundStyle(EmmColors.textSecondary)
                        .padding(.top, EmmSpacing.s3)
                }
            }
            .padding(EmmSpacing.s4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(EmmColors.surface1, in: EmmRadii.rM)
            .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
            .accessibilityElement(children: .combine)
        }

        private func statRow(label: String, value: String) -> some View {
            HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                Text(label)
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                Spacer(minLength: EmmSpacing.s2)
                Text(value)
                    .emmTextStyle(EmmType.amountS)
                    .foregroundStyle(EmmColors.textPrimary)
            }
            .padding(.bottom, EmmSpacing.s2)
        }
    }

    struct PaymentRow: View {
        let payment: LoanPaymentRowUi
        let onEdit: () -> Void
        let onDelete: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                HStack(spacing: EmmSpacing.s3) {
                    details
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .accessibilityElement(children: .combine)
                    menu
                }
                .padding(.horizontal, EmmSpacing.s4)
                .padding(.vertical, EmmSpacing.s3)
                Hairline()
            }
        }

        private var details: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: EmmSpacing.s2) {
                        paidAt
                        methodPill
                    }
                    VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                        paidAt
                        methodPill
                    }
                }
                Text(payment.amount)
                    .emmTextStyle(EmmType.amountLead)
                    .foregroundStyle(EmmColors.textPrimary)
                if !payment.note.isEmpty {
                    Text(payment.note)
                        .emmTextStyle(EmmType.bodyM)
                        .italic()
                        .foregroundStyle(EmmColors.textSecondary)
                }
            }
        }

        private var paidAt: some View {
            Text(payment.readablePaidAt)
                .emmTextStyle(EmmType.caption)
                .foregroundStyle(EmmColors.textTertiary)
        }

        private var methodPill: some View {
            Text(payment.methodLabel)
                .emmTextStyle(EmmType.labelM)
                .foregroundStyle(EmmColors.textSecondary)
                .lineLimit(1)
                .fixedSize()
                .padding(.horizontal, EmmSpacing.s2)
                .padding(.vertical, EmmSpacing.s1)
                .overlay { EmmRadii.rFull.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
        }

        private var menu: some View {
            Menu {
                Button("Editar", action: onEdit)
                Button("Borrar", role: .destructive, action: onDelete)
            } label: {
                Image(systemName: "ellipsis")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                    .foregroundStyle(EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel("Opciones del abono")
        }
    }

    struct EmptyPayments: View {
        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Image(systemName: "banknote")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s10, height: EmmSpacing.s10)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
                Text("Sin abonos todavía")
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .padding(.top, EmmSpacing.s3)
                Text("Los abonos que registres aparecen aquí")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, EmmSpacing.s1)
            }
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.vertical, EmmSpacing.s6)
            .frame(maxWidth: .infinity)
        }
    }
}
