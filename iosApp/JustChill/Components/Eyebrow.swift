import SwiftUI

struct Eyebrow: View {
    let text: String

    var body: some View {
        Text(text.uppercased())
            .emmTextStyle(EmmType.eyebrow)
            .foregroundStyle(EmmColors.textTertiary)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityAddTraits(.isHeader)
    }
}
