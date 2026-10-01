@preconcurrency import JustChillKit
import SwiftUI

extension ReportScreen {
    private static let chartHeight: CGFloat = EmmSpacing.s16 * 2

    struct TrendsTab: View {
        let trends: TrendsUiData

        var body: some View {
            if trends.isEarlyState {
                TrendsEarlyState()
            } else {
                SavingsRate(trends: trends)
                Hairline()
                IncomeVersusSpend(trends: trends)
                if !trends.topExpenses.isEmpty {
                    Hairline()
                    TopExpenses(items: trends.topExpenses)
                }
            }
        }
    }

    struct SavingsRate: View {
        let trends: TrendsUiData

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                Eyebrow(text: "TASA DE AHORRO · \(ReportViewModelKt.TRENDS_WINDOW_MONTHS) MESES")
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s3) {
                        rate
                        delta
                    }
                    VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                        rate
                        delta
                    }
                }
                if !trends.contextSentence.isEmpty {
                    Text(trends.contextSentence)
                        .emmTextStyle(EmmType.bodyM)
                        .foregroundStyle(EmmColors.textSecondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }

        private var rate: some View {
            HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s0) {
                Text(String(trends.savingsRatePercent))
                    .emmTextStyle(EmmType.amountHero)
                    .foregroundStyle(trends.isSavingsRateDeficit ? EmmColors.danger : EmmColors.textPrimary)
                Text("%")
                    .emmTextStyle(EmmType.amountL)
                    .foregroundStyle(EmmColors.textTertiary)
            }
            .lineLimit(1)
            .accessibilityElement(children: .combine)
        }

        @ViewBuilder
        private var delta: some View {
            if let deltaText = trends.deltaText {
                Pill(
                    text: deltaText,
                    symbol: trends.deltaIsPositive.map { $0.boolValue ? "arrow.up" : "arrow.down" },
                    isTinted: trends.deltaIsPositive?.boolValue == true
                )
            }
        }
    }

    struct IncomeVersusSpend: View {
        let trends: TrendsUiData

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s4) {
                HeaderRow(title: "ENTRÓ VS SALIÓ") {
                    HStack(spacing: EmmSpacing.s3) {
                        LegendDot(color: EmmColors.catSage, label: "Entró")
                        LegendDot(color: EmmColors.catTerracotta, label: "Salió")
                    }
                }
                HStack(alignment: .bottom, spacing: EmmSpacing.s0) {
                    ForEach(trends.monthlyBars, id: \.monthShortLabel) { bar in
                        BarGroup(bar: bar)
                    }
                }
                .frame(height: ReportScreen.chartHeight, alignment: .bottom)
                .accessibilityHidden(true)
                HStack(spacing: EmmSpacing.s0) {
                    ForEach(trends.monthlyBars, id: \.monthShortLabel) { bar in
                        Text(bar.monthShortLabel)
                            .emmTextStyle(EmmType.caption)
                            .lineLimit(1)
                            .minimumScaleFactor(0.5)
                            .foregroundStyle(bar.isCurrentMonth ? EmmColors.textPrimary : EmmColors.textSecondary)
                            .frame(maxWidth: .infinity)
                            .accessibilityLabel(
                                "\(bar.monthShortLabel): entró \(bar.incomeFormatted), salió \(bar.expenseFormatted)")
                    }
                }
                Hairline()
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline) {
                        averageLabel
                        Spacer()
                        averageAmounts
                    }
                    VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                        averageLabel
                        averageAmounts
                    }
                }
            }
        }

        private var averageLabel: some View {
            Text("Promedio mensual")
                .emmTextStyle(EmmType.labelM)
                .foregroundStyle(EmmColors.textSecondary)
        }

        private var averageAmounts: some View {
            Text(trends.averageIncomeFormatted + " · " + trends.averageExpenseFormatted)
                .emmTextStyle(EmmType.amountS)
                .foregroundStyle(EmmColors.textSecondary)
        }
    }

    struct BarGroup: View {
        let bar: MonthlyBarItem

        var body: some View {
            HStack(alignment: .bottom, spacing: EmmSpacing.s1) {
                column(fraction: bar.incomeFraction, color: EmmColors.catSage)
                column(fraction: bar.expenseFraction, color: EmmColors.catTerracotta)
            }
            .frame(maxWidth: .infinity, alignment: .bottom)
        }

        private func column(fraction: Float, color: Color) -> some View {
            EmmRadii.rXS
                .fill(color)
                .frame(width: EmmSpacing.s3, height: ReportScreen.chartHeight * CGFloat(fraction))
        }
    }

    struct LegendDot: View {
        let color: Color
        let label: String

        var body: some View {
            HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s1) {
                Circle()
                    .fill(color)
                    .frame(width: EmmSpacing.s2, height: EmmSpacing.s2)
                    .alignmentGuide(.firstTextBaseline) { dimensions in dimensions[.bottom] }
                Text(label)
                    .emmTextStyle(EmmType.labelM)
                    .foregroundStyle(EmmColors.textSecondary)
            }
        }
    }

    struct TopExpenses: View {
        let items: [TopCategoryItem]

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s4) {
                HeaderRow(title: "TUS MAYORES GASTOS") {
                    Text("Promedio mensual")
                        .emmTextStyle(EmmType.bodyM)
                        .foregroundStyle(EmmColors.textSecondary)
                }
                ForEach(items, id: \.categoryId) { item in
                    TopExpenseRow(item: item)
                }
            }
        }
    }

    struct TopExpenseRow: View {
        let item: TopCategoryItem

        var body: some View {
            HStack(spacing: EmmSpacing.s3) {
                Image(systemName: EmmCategory.resolvedSymbol(item.iconKey))
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                    .foregroundStyle(EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s10, height: EmmSpacing.s10)
                    .background(EmmColors.surface1, in: EmmRadii.rS)
                    .accessibilityHidden(true)
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                        labels
                        Spacer(minLength: EmmSpacing.s2)
                        total
                    }
                    VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                        labels
                        total
                    }
                }
            }
            .accessibilityElement(children: .combine)
        }

        private var labels: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                Text(item.name)
                    .emmTextStyle(EmmType.labelL)
                    .foregroundStyle(EmmColors.textPrimary)
                Text(item.topMetaText)
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
            }
        }

        private var total: some View {
            Text(item.totalFormatted)
                .emmTextStyle(EmmType.amountS)
                .foregroundStyle(EmmColors.textPrimary)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
        }
    }

    struct TrendsEarlyState: View {
        var body: some View {
            VStack(spacing: EmmSpacing.s3) {
                Image(systemName: "chart.line.uptrend.xyaxis")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
                Text("Vuelve cuando tengas más historial")
                    .emmTextStyle(EmmType.headlineM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .multilineTextAlignment(.center)
                Text("Tendencias necesita al menos 3 meses para tener algo útil que mostrar.")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, EmmSpacing.s12)
            .accessibilityElement(children: .combine)
        }
    }
}
