@preconcurrency import JustChillKit
import SwiftUI

extension ReportScreen {
    struct MonthTab: View {
        let state: ReportUiState
        let send: Send
        let onAddTransaction: () -> Void

        var body: some View {
            MonthNavigator(state: state, send: send)
            if state.isMonthEmpty {
                MonthEmptyState(month: state.month.monthYearLabel())
            } else {
                Segmented(options: typeOptions)
                if state.isEmpty {
                    EmptyState(isIncome: isIncome, onAddTransaction: onAddTransaction)
                } else {
                    Hero(state: state, isIncome: isIncome)
                    CategoryShares(state: state)
                }
            }
        }

        private var isIncome: Bool {
            state.selectedType == TransactionType.income
        }

        private var typeOptions: [Segmented.Option] {
            [
                Segmented.Option(
                    label: "Ingresos",
                    isSelected: isIncome,
                    select: { send(ReportIntentSelectType(type: TransactionType.income)) }
                ),
                Segmented.Option(
                    label: "Gastos",
                    isSelected: !isIncome,
                    select: { send(ReportIntentSelectType(type: TransactionType.spend)) }
                ),
            ]
        }
    }

    struct MonthNavigator: View {
        let state: ReportUiState
        let send: Send

        var body: some View {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: EmmSpacing.s2) {
                    selector
                    if !state.isCurrentMonth { todayButton }
                }
                VStack(spacing: EmmSpacing.s2) {
                    selector
                    if !state.isCurrentMonth { todayButton }
                }
            }
            .frame(maxWidth: .infinity)
        }

        private var selector: some View {
            HStack(spacing: EmmSpacing.s1) {
                chevron(symbol: "chevron.left", label: "Mes anterior", intent: ReportIntentPreviousMonth.shared)
                Button {
                    send(ReportIntentOnMonthSheetRequested.shared)
                } label: {
                    HStack(spacing: EmmSpacing.s1) {
                        Text(state.month.monthYearLabel())
                            .emmTextStyle(EmmType.labelL)
                            .foregroundStyle(EmmColors.textPrimary)
                            .multilineTextAlignment(.center)
                        Image(systemName: "chevron.down")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                            .foregroundStyle(EmmColors.textTertiary)
                    }
                    .padding(.horizontal, EmmSpacing.s2)
                    .frame(minHeight: EmmSpacing.s12)
                    .contentShape(Rectangle())
                }
                .accessibilityLabel(state.month.monthYearLabel() + ". Cambiar de mes")
                chevron(symbol: "chevron.right", label: "Mes siguiente", intent: ReportIntentNextMonth.shared)
            }
            .padding(.horizontal, EmmSpacing.s1)
            .overlay { EmmRadii.rFull.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
        }

        private var todayButton: some View {
            Button {
                send(ReportIntentJumpToCurrent.shared)
            } label: {
                Text("Hoy")
                    .emmTextStyle(EmmType.labelL)
                    .foregroundStyle(EmmColors.textPrimary)
                    .padding(.horizontal, EmmSpacing.s4)
                    .frame(minHeight: EmmSpacing.s12)
                    .overlay { EmmRadii.rFull.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                    .contentShape(Rectangle())
            }
        }

        private func chevron(symbol: String, label: String, intent: any ReportIntent) -> some View {
            Button {
                send(intent)
            } label: {
                Image(systemName: symbol)
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                    .foregroundStyle(EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
            }
            .accessibilityLabel(label)
        }
    }

    struct Hero: View {
        let state: ReportUiState
        let isIncome: Bool

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                Eyebrow(text: eyebrowText)
                Text(state.totalFormatted)
                    .emmTextStyle(EmmType.amountL)
                    .foregroundStyle(isIncome ? EmmColors.success : EmmColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                if let pillText = state.comparisonPillText, let comparisonText = state.comparisonText {
                    ViewThatFits(in: .horizontal) {
                        HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                            pill(text: pillText)
                            comparisonLabel(comparisonText)
                        }
                        VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                            pill(text: pillText)
                            comparisonLabel(comparisonText)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }

        private var eyebrowText: String {
            let month: String = state.month.monthLabel().uppercased()
            return (isIncome ? "TOTAL INGRESOS · " : "TOTAL GASTOS · ") + month
        }

        private func pill(text: String) -> some View {
            Pill(
                text: text,
                symbol: state.comparisonDirectionUp.map { $0.boolValue ? "arrow.up" : "arrow.down" },
                isTinted: state.comparisonIsPositive?.boolValue == true
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(state.comparisonPillDescription ?? text)
        }

        private func comparisonLabel(_ text: String) -> some View {
            Text(text)
                .emmTextStyle(EmmType.bodyM)
                .foregroundStyle(EmmColors.textSecondary)
        }
    }

    struct CategoryShares: View {
        let state: ReportUiState

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s4) {
                Hairline()
                HeaderRow(title: "POR CATEGORÍA") {
                    Text(categoryCountText)
                        .emmTextStyle(EmmType.bodyM)
                        .foregroundStyle(EmmColors.textSecondary)
                }
                ForEach(state.shares, id: \.categoryId) { share in
                    ShareRow(share: share)
                }
                Hairline()
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline) {
                        movementLabel
                        Spacer()
                        averageLabel
                    }
                    VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                        movementLabel
                        averageLabel
                    }
                }
            }
        }

        private var categoryCountText: String {
            "\(state.shares.count) " + (state.shares.count == 1 ? "categoría" : "categorías")
        }

        private var movementLabel: some View {
            Text("\(state.movementCount) " + (state.movementCount == 1 ? "movimiento" : "movimientos"))
                .emmTextStyle(EmmType.caption)
                .foregroundStyle(EmmColors.textSecondary)
        }

        private var averageLabel: some View {
            Text("Promedio " + state.averageFormatted)
                .emmTextStyle(EmmType.caption)
                .foregroundStyle(EmmColors.textSecondary)
        }
    }

    struct ShareRow: View {
        let share: CategoryShare

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                        dot
                        name
                        Spacer(minLength: EmmSpacing.s2)
                        amount
                        percent
                    }
                    VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                        HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                            dot
                            name
                        }
                        HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                            amount
                            percent
                        }
                    }
                }
                GeometryReader { proxy in
                    EmmRadii.rFull
                        .fill(color)
                        .frame(width: proxy.size.width * CGFloat(share.fraction))
                }
                .frame(height: EmmSpacing.s1)
                .accessibilityHidden(true)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(
                "\(share.name): \(share.amountFormatted), \(share.percentage) por ciento del total")
        }

        private var color: Color {
            EmmCategory.resolvedColor(share.colorKey)
        }

        private var dot: some View {
            Circle()
                .fill(color)
                .frame(width: EmmSpacing.s2, height: EmmSpacing.s2)
                .alignmentGuide(.firstTextBaseline) { dimensions in dimensions[.bottom] }
        }

        private var name: some View {
            Text(share.name)
                .emmTextStyle(EmmType.bodyL)
                .foregroundStyle(EmmColors.textPrimary)
        }

        private var amount: some View {
            Text(share.amountFormatted)
                .emmTextStyle(EmmType.amountS)
                .foregroundStyle(EmmColors.textPrimary)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
        }

        private var percent: some View {
            Text("\(share.percentage)%")
                .emmTextStyle(EmmType.caption)
                .foregroundStyle(EmmColors.textSecondary)
        }
    }

    struct MonthEmptyState: View {
        let month: String

        var body: some View {
            VStack(spacing: EmmSpacing.s3) {
                Image(systemName: "calendar")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                    .foregroundStyle(EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .background(EmmColors.surface1, in: EmmRadii.rM)
                    .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                    .accessibilityHidden(true)
                Text("Sin movimientos en " + month)
                    .emmTextStyle(EmmType.headlineM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .multilineTextAlignment(.center)
                Text("Anota un gasto o ingreso para empezar a ver tu reporte de este mes.")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, EmmSpacing.s12)
            .accessibilityElement(children: .combine)
        }
    }

    struct EmptyState: View {
        let isIncome: Bool
        let onAddTransaction: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s3) {
                Image(systemName: "receipt")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
                Text(isIncome ? "Aún no registraste ingresos este mes" : "Aún no registraste gastos este mes")
                    .emmTextStyle(EmmType.headlineM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .multilineTextAlignment(.center)
                Text("Anota el primero y vuelve al final del mes")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                    .multilineTextAlignment(.center)
                Button(action: onAddTransaction) {
                    Text(isIncome ? "Anotar ingreso" : "Anotar gasto")
                        .emmTextStyle(EmmType.labelL)
                        .foregroundStyle(EmmColors.bg)
                        .padding(.horizontal, EmmSpacing.s5)
                        .frame(minHeight: EmmSpacing.s12)
                        .background(EmmColors.textPrimary, in: EmmRadii.rXS)
                }
                .padding(.top, EmmSpacing.s4)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, EmmSpacing.s12)
        }
    }
}
