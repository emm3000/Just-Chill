@preconcurrency import JustChillKit
import SwiftUI

extension EditMovementScreen {
    struct TopBar: View {
        let isSpend: Bool
        let onClose: () -> Void
        let onDelete: () -> Void

        var body: some View {
            HStack(spacing: EmmSpacing.s0) {
                iconButton(symbol: "xmark", color: EmmColors.textSecondary, label: "Cerrar", action: onClose)
                Text(isSpend ? "Editar gasto" : "Editar ingreso")
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity)
                    .accessibilityAddTraits(.isHeader)
                iconButton(
                    symbol: "trash", color: EmmColors.danger,
                    label: isSpend ? "Eliminar gasto" : "Eliminar ingreso", action: onDelete)
            }
            .padding(.horizontal, EmmSpacing.s4)
        }

        private func iconButton(symbol: String, color: Color, label: String, action: @escaping () -> Void)
            -> some View
        {
            Button(action: action) {
                Image(systemName: symbol)
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                    .foregroundStyle(color)
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(label)
        }
    }

    struct NoteRow: View {
        let note: String
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                    Image(systemName: "text.alignleft")
                        .foregroundStyle(EmmColors.textTertiary)
                        .accessibilityHidden(true)
                    Text(note.isEmpty ? "Agregar nota" : note)
                        .emmTextStyle(EmmType.bodyM)
                        .foregroundStyle(note.isEmpty ? EmmColors.textTertiary : EmmColors.textSecondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
                .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12, alignment: .leading)
                .contentShape(Rectangle())
            }
            .accessibilityHint(note.isEmpty ? "Agregar una nota" : "Editar la nota")
            .padding(.horizontal, EmmSpacing.s6)
        }
    }
}
