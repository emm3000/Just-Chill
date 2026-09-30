import SwiftUI

struct Hairline: View {
    var isFocused: Bool = false

    var body: some View {
        Rectangle()
            .fill(isFocused ? EmmColors.borderFocus : EmmColors.border)
            .frame(height: EmmSpacing.hairline)
            .accessibilityHidden(true)
    }
}
