@preconcurrency import JustChillKit
import SwiftUI

extension MovementsScreen {
    struct DayList: View {
        let state: SeeTransactionsUiState
        let send: Send
        let onEdit: (String) -> Void

        var body: some View {
            switch state.listDisplayState {
            case .loading:
                Color.clear.frame(maxWidth: .infinity, maxHeight: .infinity)
            case .emptyLedger:
                EmptyLedger()
            case .emptyMonth:
                EmptyMonth()
            case .noSearchResults:
                NoResults(state: state, send: send)
            case .content:
                content
            }
        }

        private var content: some View {
            ScrollView {
                LazyVStack(spacing: EmmSpacing.s0, pinnedViews: [.sectionHeaders]) {
                    ForEach(state.days, id: \.date) { day in
                        Section {
                            ForEach(day.transactions, id: \.transactionId) { transaction in
                                Row(transaction: transaction) { onEdit(transaction.transactionId) }
                            }
                        } header: {
                            DayHeader(day: day, showsMonthYear: state.isFilterActive)
                        }
                    }
                }
                .padding(.bottom, EmmSpacing.s4)
            }
        }
    }

    struct DayHeader: View {
        let day: DayGroup
        let showsMonthYear: Bool

        var body: some View {
            HStack(alignment: .bottom) {
                Text(day.primaryLabel.uppercased())
                    .emmTextStyle(EmmType.eyebrow)
                    .foregroundStyle(EmmColors.textTertiary)
                    .lineLimit(1)
                Spacer()
                if showsMonthYear {
                    Text(day.monthYearCaption)
                        .emmTextStyle(EmmType.caption)
                        .foregroundStyle(EmmColors.textDisabled)
                } else if let spendTotal = day.spendTotalCents?.int64Value {
                    Text(CurrencyFormatKt.formatNeutral(value: MoneyFormatterKt.format(spendTotal)))
                        .emmTextStyle(EmmType.amountS)
                        .foregroundStyle(EmmColors.textTertiary)
                }
            }
            .padding(.top, EmmSpacing.s4)
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.bottom, EmmSpacing.s2)
            .frame(maxWidth: .infinity)
            .background(EmmColors.bg)
            .accessibilityAddTraits(.isHeader)
        }
    }

    struct Row: View {
        let transaction: TransactionUi
        let onOpen: () -> Void

        var body: some View {
            Button(action: onOpen) { layout }
                .buttonStyle(.plain)
        }

        private var layout: some View {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: EmmSpacing.s3) {
                    tile
                    texts(subtitleLines: 1).frame(maxWidth: .infinity, alignment: .leading)
                    amount
                }
                VStack(alignment: .trailing, spacing: EmmSpacing.s1) {
                    HStack(spacing: EmmSpacing.s3) {
                        tile
                        texts(subtitleLines: 2).frame(maxWidth: .infinity, alignment: .leading)
                    }
                    amount
                }
            }
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.vertical, EmmSpacing.s2)
            .contentShape(Rectangle())
            .accessibilityElement(children: .combine)
        }

        private var tile: some View {
            Image(systemName: EmmCategory.resolvedSymbol(transaction.category.iconId))
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                .foregroundStyle(EmmColors.textSecondary)
                .frame(width: EmmSpacing.s10, height: EmmSpacing.s10)
                .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .accessibilityHidden(true)
        }

        private func texts(subtitleLines: Int) -> some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                line(
                    text: transaction.title, style: EmmType.labelL, color: EmmColors.textPrimary, dotsHere: true,
                    lines: 1)
                line(
                    text: transaction.subtitle, style: EmmType.caption, color: EmmColors.textTertiary, dotsHere: false,
                    lines: subtitleLines)
            }
        }

        private var amount: some View {
            Text(transaction.amount)
                .emmTextStyle(EmmType.amountM)
                .foregroundStyle(
                    transaction.type == TransactionType.income ? EmmColors.success : EmmColors.textPrimary
                )
                .fixedSize(horizontal: true, vertical: false)
        }

        private func line(text: String, style: EmmTextStyle, color: Color, dotsHere: Bool, lines: Int) -> some View {
            HStack(spacing: EmmSpacing.s2) {
                Circle()
                    .fill(
                        dotsHere == transaction.categoryLeadsTitle
                            ? EmmCategory.resolvedColor(transaction.category.colorId) : Color.clear
                    )
                    .frame(width: EmmSpacing.s2, height: EmmSpacing.s2)
                Text(text)
                    .emmTextStyle(style)
                    .foregroundStyle(color)
                    .lineLimit(lines)
            }
        }
    }

    struct EmptyLedger: View {
        var body: some View {
            VStack(spacing: EmmSpacing.s2) {
                Image(systemName: "receipt")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s6, height: EmmSpacing.s6)
                    .foregroundStyle(EmmColors.textTertiary)
                    .frame(width: EmmSpacing.s16, height: EmmSpacing.s16)
                    .background(EmmColors.surface1, in: EmmRadii.rXL)
                    .overlay { EmmRadii.rXL.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                    .padding(.bottom, EmmSpacing.s3)
                    .accessibilityHidden(true)
                Text("Aún sin transacciones")
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.textPrimary)
                Text("Las que registres aparecerán acá agrupadas por día.")
                    .emmTextStyle(EmmType.caption)
                    .foregroundStyle(EmmColors.textTertiary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, EmmSpacing.s6)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    struct EmptyMonth: View {
        var body: some View {
            VStack(spacing: EmmSpacing.s2) {
                Image(systemName: "calendar")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s6, height: EmmSpacing.s6)
                    .foregroundStyle(EmmColors.textTertiary)
                    .padding(.bottom, EmmSpacing.s3)
                    .accessibilityHidden(true)
                Text("Sin movimientos este mes")
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.textPrimary)
                Text("Toca el mes de arriba para cambiarlo.")
                    .emmTextStyle(EmmType.caption)
                    .foregroundStyle(EmmColors.textTertiary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, EmmSpacing.s6)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    struct NoResults: View {
        let state: SeeTransactionsUiState
        let send: Send

        var body: some View {
            VStack(spacing: EmmSpacing.s2) {
                Text(headline)
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .multilineTextAlignment(.center)
                Text("Prueba con otro nombre, otro monto, o limpia los filtros activos.")
                    .emmTextStyle(EmmType.caption)
                    .foregroundStyle(EmmColors.textTertiary)
                    .multilineTextAlignment(.center)
                Button {
                    send(SeeTransactionsIntentOnClearFilters.shared)
                } label: {
                    HStack(spacing: EmmSpacing.s2) {
                        Image(systemName: "xmark")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                        Text("Limpiar filtros")
                            .emmTextStyle(EmmType.labelM)
                    }
                    .foregroundStyle(EmmColors.textPrimary)
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.vertical, EmmSpacing.s2)
                    .overlay { EmmRadii.rFull.stroke(EmmColors.borderFocus, lineWidth: EmmSpacing.hairline) }
                    .frame(minHeight: EmmSpacing.s12)
                }
                .padding(.top, EmmSpacing.s3)
            }
            .padding(.horizontal, EmmSpacing.s6)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }

        private var headline: String {
            if let category = state.activeCategory {
                return "Sin resultados para «" + category.name + "»"
            }
            if !state.query.isEmpty {
                return "Sin resultados para «" + state.query + "»"
            }
            return "Sin movimientos con esos filtros"
        }
    }
}
