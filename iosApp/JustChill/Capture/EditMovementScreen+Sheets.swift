@preconcurrency import JustChillKit
import SwiftUI

extension EditMovementScreen {
    struct NoteSheet: View {
        let onSave: (String) -> Void
        let onDismiss: () -> Void
        @State private var draft: String

        init(initialNote: String, onSave: @escaping (String) -> Void, onDismiss: @escaping () -> Void) {
            self.onSave = onSave
            self.onDismiss = onDismiss
            _draft = State(initialValue: initialNote)
        }

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                SheetTitleBar(title: "Nota", onClose: onDismiss)
                TextField(
                    "", text: $draft, prompt: Text("Opcional").foregroundStyle(EmmColors.textTertiary),
                    axis: .vertical
                )
                .emmTextStyle(EmmType.bodyM)
                .foregroundStyle(EmmColors.textPrimary)
                .tint(EmmColors.borderFocus)
                .lineLimit(3...6)
                .padding(EmmSpacing.s3)
                .background(EmmColors.surface1, in: EmmRadii.rM)
                .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .padding(.horizontal, EmmSpacing.s5)
                Text("Se guarda al confirmar el movimiento")
                    .emmTextStyle(EmmType.caption)
                    .foregroundStyle(EmmColors.textTertiary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, EmmSpacing.s6)
                    .padding(.top, EmmSpacing.s2)
                Spacer(minLength: EmmSpacing.s0)
                Button {
                    onSave(draft)
                    onDismiss()
                } label: {
                    Text("Guardar nota")
                        .emmTextStyle(EmmType.titleM)
                        .foregroundStyle(EmmColors.bg)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
                        .background(EmmColors.textPrimary, in: EmmRadii.rL)
                }
                .padding(.horizontal, EmmSpacing.s4)
                .padding(.bottom, EmmSpacing.s4)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents([.medium])
        }
    }

    struct DeleteSheet: View {
        let isSpend: Bool
        let amount: String
        let category: SelectableCategory?
        let accountName: String?
        let onConfirm: () -> Void
        let onDismiss: () -> Void
        @State private var contentHeight: CGFloat = EmmSpacing.s0

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s3) {
                Text(isSpend ? "¿Eliminar este gasto?" : "¿Eliminar este ingreso?")
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Text("Esta acción no se puede deshacer.")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                summary
                Button(action: onConfirm) {
                    Text("Eliminar")
                        .emmTextStyle(EmmType.titleM)
                        .foregroundStyle(EmmColors.danger)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
                        .overlay { EmmRadii.rL.stroke(EmmColors.danger, lineWidth: EmmSpacing.hairline) }
                }
                .padding(.top, EmmSpacing.s3)
                Button(action: onDismiss) {
                    Text("Cancelar")
                        .emmTextStyle(EmmType.titleM)
                        .foregroundStyle(EmmColors.textSecondary)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12)
                }
            }
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.top, EmmSpacing.s6)
            .padding(.bottom, EmmSpacing.s4)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .onGeometryChange(for: CGFloat.self) {
                $0.size.height
            } action: {
                contentHeight = $0
            }
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents(contentHeight > EmmSpacing.s0 ? [.height(contentHeight)] : [.medium])
        }

        private var summary: some View {
            HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                    if let category {
                        Circle()
                            .fill(EmmCategory.resolvedColor(category.colorId))
                            .frame(width: EmmSpacing.s2, height: EmmSpacing.s2)
                            .alignmentGuide(.firstTextBaseline) { dimensions in dimensions[.bottom] }
                    }
                    Text(summaryLabel)
                        .emmTextStyle(EmmType.labelL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Text(CurrencyFormatKt.formatNeutral(value: CentsFormatterKt.formatCentsForDisplay(digits: amount)))
                    .emmTextStyle(EmmType.amountM)
                    .foregroundStyle(isSpend ? EmmColors.textPrimary : EmmColors.success)
                    .fixedSize(horizontal: true, vertical: false)
            }
            .padding(EmmSpacing.s3)
            .background(EmmColors.surface2, in: EmmRadii.rL)
            .overlay { EmmRadii.rL.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
            .accessibilityElement(children: .combine)
        }

        private var summaryLabel: String {
            let name: String = category?.name ?? "—"
            guard let accountName else { return name }
            return name + " / " + accountName
        }
    }
}
