@preconcurrency import JustChillKit
import SwiftUI

struct Numpad: View {
    @Binding var digits: String

    private let keys: [[String]] = [["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"], ["00", "0", "delete"]]

    var body: some View {
        VStack(spacing: EmmSpacing.s2) {
            ForEach(keys, id: \.self) { row in
                HStack(spacing: EmmSpacing.s2) {
                    ForEach(row, id: \.self) { key in
                        Button {
                            press(key)
                        } label: {
                            glyph(key)
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func glyph(_ key: String) -> some View {
        let isEditingKey: Bool = key == "00" || key == "delete"
        Group {
            if key == "delete" {
                Image(systemName: "delete.left")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                    .foregroundStyle(EmmColors.textSecondary)
                    .accessibilityLabel("Borrar")
            } else {
                Text(key)
                    .emmTextStyle(EmmType.amountLead)
                    .foregroundStyle(EmmColors.textPrimary)
            }
        }
        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
        .background(isEditingKey ? EmmColors.surface1 : Color.clear, in: EmmRadii.rM)
        .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
    }

    private func press(_ key: String) {
        let limit: Int = Int(CentsFormatterKt.MAX_AMOUNT_DIGITS)
        switch key {
        case "delete": digits = String(digits.dropLast())
        default: digits = String((digits + key).prefix(limit))
        }
    }
}
