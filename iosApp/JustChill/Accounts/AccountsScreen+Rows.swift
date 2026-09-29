@preconcurrency import JustChillKit
import SwiftUI

extension AccountsScreen {
    struct Row: View {
        let row: AccountMonthUi
        let send: Send

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                HStack(spacing: EmmSpacing.s0) {
                    content
                    menu
                }
                .padding(.leading, EmmSpacing.s6)
                .padding(.trailing, EmmSpacing.s2)
                .padding(.vertical, EmmSpacing.s2)
                Hairline()
            }
        }

        private var content: some View {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: EmmSpacing.s3) {
                    TypeTile(symbol: row.account.type.symbolName)
                    HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s3) {
                        texts(subtitleLines: 1)
                        Spacer(minLength: EmmSpacing.s0)
                        net
                    }
                }
                VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                    HStack(spacing: EmmSpacing.s3) {
                        TypeTile(symbol: row.account.type.symbolName)
                        texts(subtitleLines: 2)
                    }
                    net
                }
            }
            .accessibilityElement(children: .combine)
        }

        private func texts(subtitleLines: Int) -> some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                Text(row.account.name)
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .lineLimit(1)
                Text(subtitle)
                    .emmTextStyle(EmmType.labelM)
                    .foregroundStyle(EmmColors.textTertiary)
                    .lineLimit(subtitleLines)
            }
        }

        private var net: some View {
            VStack(alignment: .trailing, spacing: EmmSpacing.s1) {
                Text(row.net)
                    .emmTextStyle(EmmType.amountM)
                    .foregroundStyle(netColor)
                    .fixedSize(horizontal: true, vertical: false)
                Text("este mes")
                    .emmTextStyle(EmmType.caption)
                    .foregroundStyle(EmmColors.textTertiary)
            }
        }

        private var menu: some View {
            Menu {
                Button("Editar") { send(AccountsIntentOnEditClick(account: row.account)) }
                Button("Borrar", role: .destructive) { send(AccountsIntentOnDeleteClick(account: row.account)) }
            } label: {
                Image(systemName: "ellipsis")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                    .foregroundStyle(EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel("Opciones de cuenta")
        }

        private var subtitle: String {
            switch row.movementCount {
            case 0: "Sin movimientos este mes"
            case 1: row.account.type.label + " · 1 movimiento"
            default: row.account.type.label + " · \(row.movementCount) movimientos"
            }
        }

        private var netColor: Color {
            switch row.netTone {
            case .muted: EmmColors.textTertiary
            case .positive: EmmColors.success
            case .neutral: EmmColors.textPrimary
            }
        }
    }

    struct TypeTile: View {
        let symbol: String

        var body: some View {
            Image(systemName: symbol)
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                .foregroundStyle(EmmColors.textSecondary)
                .frame(width: EmmSpacing.s10, height: EmmSpacing.s10)
                .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .accessibilityHidden(true)
        }
    }

    struct LoansRow: View {
        let state: AccountsUiState
        let onOpen: () -> Void

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                Eyebrow(text: "Préstamos")
                    .padding(.horizontal, EmmSpacing.s6)
                    .padding(.top, EmmSpacing.s6)
                    .padding(.bottom, EmmSpacing.s2)
                Button(action: onOpen) { layout }
                    .buttonStyle(.plain)
                    .accessibilityHint("Ver préstamos")
            }
        }

        private var layout: some View {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: EmmSpacing.s3) {
                    TypeTile(symbol: "person.2")
                    HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s3) {
                        texts(subtitleLines: 1)
                        Spacer(minLength: EmmSpacing.s0)
                        total
                    }
                    chevron
                }
                VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                    HStack(spacing: EmmSpacing.s3) {
                        TypeTile(symbol: "person.2")
                        texts(subtitleLines: 2)
                    }
                    total
                }
            }
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.vertical, EmmSpacing.s3)
            .frame(minHeight: EmmSpacing.s12)
            .contentShape(Rectangle())
            .accessibilityElement(children: .combine)
        }

        private func texts(subtitleLines: Int) -> some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                Text("Te deben")
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.textPrimary)
                Text(subtitle)
                    .emmTextStyle(EmmType.labelM)
                    .foregroundStyle(EmmColors.textTertiary)
                    .lineLimit(subtitleLines)
            }
        }

        private var total: some View {
            Text(state.loansTotalOwed)
                .emmTextStyle(EmmType.amountM)
                .foregroundStyle(state.loansTotalOwedIsPositive ? EmmColors.success : EmmColors.textTertiary)
                .fixedSize(horizontal: true, vertical: false)
        }

        private var chevron: some View {
            Image(systemName: "chevron.right")
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                .foregroundStyle(EmmColors.textTertiary)
                .accessibilityHidden(true)
        }

        private var subtitle: String {
            let people: [String] = state.loansPeople
            switch people.count {
            case 0: return "Nadie te debe"
            case 1: return "1 persona · " + people[0]
            default: return "\(people.count) personas · " + people.joined(separator: ", ")
            }
        }
    }

    struct EmptyState: View {
        let onCreate: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Image(systemName: "wallet.bifold")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s8, height: EmmSpacing.s8)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
                Text("Aún sin cuentas")
                    .emmTextStyle(EmmType.titleL)
                    .foregroundStyle(EmmColors.textPrimary)
                    .padding(.top, EmmSpacing.s3)
                Text("Crea una para empezar a registrar movimientos")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, EmmSpacing.s1)
                Button(action: onCreate) {
                    Text("Crear cuenta")
                        .emmTextStyle(EmmType.labelL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .padding(.horizontal, EmmSpacing.s6)
                        .frame(minHeight: EmmSpacing.s12)
                        .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                        .contentShape(Rectangle())
                }
                .padding(.top, EmmSpacing.s5)
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.vertical, EmmSpacing.s8)
        }
    }
}
