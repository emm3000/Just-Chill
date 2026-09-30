@preconcurrency import JustChillKit
import SwiftUI

extension MovementsScreen {
    struct Header: View {
        let isFilterActive: Bool
        let send: Send

        var body: some View {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: EmmSpacing.s2) {
                    title
                    Spacer()
                    buttons
                }
                VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                    title
                    HStack(spacing: EmmSpacing.s2) {
                        Spacer()
                        buttons
                    }
                }
            }
            .padding(.horizontal, EmmSpacing.s6)
            .frame(minHeight: EmmSpacing.s16)
        }

        private var title: some View {
            Text("Movimientos")
                .emmTextStyle(EmmType.titleL)
                .foregroundStyle(EmmColors.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .accessibilityAddTraits(.isHeader)
        }

        @ViewBuilder
        private var buttons: some View {
            HeaderButton(
                symbol: "magnifyingglass",
                label: "Buscar transacciones",
                showsBadge: false,
                action: { send(SeeTransactionsIntentScreenChromeIntentOnSearchRequested.shared) }
            )
            HeaderButton(
                symbol: "line.3.horizontal.decrease",
                label: isFilterActive ? "Filtrar movimientos, filtro activo" : "Filtrar movimientos",
                showsBadge: isFilterActive,
                action: { send(SeeTransactionsIntentScreenChromeIntentOnFilterSheetRequested.shared) }
            )
        }
    }

    struct HeaderButton: View {
        let symbol: String
        let label: String
        let showsBadge: Bool
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                Image(systemName: symbol)
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                    .foregroundStyle(EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                    .overlay(alignment: .topTrailing) {
                        if showsBadge {
                            Circle()
                                .fill(EmmColors.textPrimary)
                                .frame(width: EmmSpacing.s2, height: EmmSpacing.s2)
                                .padding(EmmSpacing.s2)
                        }
                    }
            }
            .accessibilityLabel(label)
        }
    }

    struct SearchBar: View {
        let query: String
        let send: Send
        @FocusState private var isFocused: Bool

        var body: some View {
            HStack(spacing: EmmSpacing.s2) {
                HStack(spacing: EmmSpacing.s3) {
                    Image(systemName: "magnifyingglass")
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                        .foregroundStyle(EmmColors.textTertiary)
                        .accessibilityHidden(true)
                    TextField(
                        "",
                        text: queryBinding,
                        prompt: Text("Buscar por descripción o monto").foregroundStyle(EmmColors.textTertiary)
                    )
                    .emmTextStyle(EmmType.labelL)
                    .foregroundStyle(EmmColors.textPrimary)
                    .tint(EmmColors.borderFocus)
                    .focused($isFocused)
                    .submitLabel(.search)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    if !query.isEmpty {
                        Button {
                            send(SeeTransactionsIntentOnQueryChanged(query: ""))
                        } label: {
                            Image(systemName: "xmark")
                                .resizable()
                                .scaledToFit()
                                .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                                .foregroundStyle(EmmColors.textTertiary)
                                .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                        }
                        .accessibilityLabel("Limpiar búsqueda")
                    }
                }
                .padding(.horizontal, EmmSpacing.s3)
                .frame(height: EmmSpacing.s12)
                .background(EmmColors.surface1, in: EmmRadii.rM)
                .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                HeaderButton(
                    symbol: "xmark",
                    label: "Cerrar búsqueda",
                    showsBadge: false,
                    action: { send(SeeTransactionsIntentScreenChromeIntentOnSearchClosed.shared) }
                )
            }
            .padding(.top, EmmSpacing.s3)
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.bottom, EmmSpacing.s4)
            .onAppear { isFocused = true }
        }

        private var queryBinding: Binding<String> {
            Binding(
                get: { query },
                set: { send(SeeTransactionsIntentOnQueryChanged(query: $0)) }
            )
        }
    }

    struct MonthHeader: View {
        let state: SeeTransactionsUiState
        let send: Send

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                Button {
                    send(SeeTransactionsIntentScreenChromeIntentOnMonthPickerRequested.shared)
                } label: {
                    HStack(spacing: EmmSpacing.s1) {
                        Text(eyebrowText.uppercased())
                            .emmTextStyle(EmmType.eyebrow)
                            .foregroundStyle(EmmColors.textTertiary)
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                        Image(systemName: "chevron.right")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                            .foregroundStyle(EmmColors.textTertiary)
                    }
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .accessibilityLabel(eyebrowText + ". Cambiar de mes")
                if let summary = displayedSummary {
                    MonthTotals(summary: summary, isNetPositive: state.isNetPositive)
                }
            }
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.top, EmmSpacing.s2)
            .padding(.bottom, displayedSummary == nil ? EmmSpacing.s0 : EmmSpacing.s2)
        }

        private var displayedSummary: MonthSummaryUi? {
            state.listDisplayState == ListDisplayState.content ? state.summary : nil
        }

        private var eyebrowText: String {
            let label: String = state.month.monthLabel()
            return state.isMonthYearVisible
                ? "Gastado en " + label + " " + String(state.month.year)
                : "Gastado en " + label
        }
    }

    struct MonthTotals: View {
        let summary: MonthSummaryUi
        let isNetPositive: Bool

        @ViewBuilder
        private var secondaryAmounts: some View {
            SecondaryAmount(
                label: "Entró",
                value: CurrencyFormatKt.formatNeutral(value: MoneyFormatterKt.format(summary.income)),
                color: EmmColors.textSecondary
            )
            SecondaryAmount(
                label: "Neto",
                value: CurrencyFormatKt.positiveMoneyFormatted(summary.net),
                color: isNetPositive ? EmmColors.success : EmmColors.textSecondary
            )
        }

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                    Text("S/")
                        .emmTextStyle(EmmType.amountLead)
                        .foregroundStyle(EmmColors.textPrimary)
                    Text(MoneyFormatterKt.format(summary.spend))
                        .emmTextStyle(EmmType.amountL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .lineLimit(1)
                }
                .accessibilityElement(children: .combine)
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: EmmSpacing.s4) { secondaryAmounts }
                    VStack(alignment: .leading, spacing: EmmSpacing.s1) { secondaryAmounts }
                }
            }
        }
    }

    struct SecondaryAmount: View {
        let label: String
        let value: String
        let color: Color

        var body: some View {
            HStack(spacing: EmmSpacing.s1) {
                Text(label)
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textTertiary)
                Text(value)
                    .emmTextStyle(EmmType.amountS)
                    .foregroundStyle(color)
            }
            .lineLimit(1)
            .accessibilityElement(children: .combine)
        }
    }

    struct FilterBanner: View {
        let state: SeeTransactionsUiState
        let send: Send

        var body: some View {
            HStack(spacing: EmmSpacing.s2) {
                Image(systemName: "list.bullet")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                    .foregroundStyle(EmmColors.textSecondary)
                    .accessibilityHidden(true)
                Text(bannerText)
                    .emmTextStyle(EmmType.labelM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button {
                    send(SeeTransactionsIntentOnClearCategoryFilter.shared)
                } label: {
                    HStack(spacing: EmmSpacing.s1) {
                        Text("Limpiar")
                            .emmTextStyle(EmmType.labelM)
                        Image(systemName: "xmark")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                    }
                    .foregroundStyle(EmmColors.textSecondary)
                    .padding(.horizontal, EmmSpacing.s3)
                    .frame(minHeight: EmmSpacing.s12)
                }
                .accessibilityLabel("Limpiar filtro")
            }
            .padding(.leading, EmmSpacing.s3)
            .frame(minHeight: EmmSpacing.s12)
            .background(EmmColors.surface1, in: EmmRadii.rS)
            .overlay { EmmRadii.rS.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.bottom, EmmSpacing.s3)
        }

        private var bannerText: AttributedString {
            state.filterBannerSegments.reduce(into: AttributedString()) {
                (text: inout AttributedString, segment: FilterBannerSegment) in
                var piece: AttributedString = AttributedString(segment.text)
                if segment.kind == .emphasis {
                    piece.font = Font.custom(
                        EmmFonts.interSemiBold, size: EmmType.labelM.size, relativeTo: EmmType.labelM.textStyle)
                }
                text += piece
            }
        }
    }
}
