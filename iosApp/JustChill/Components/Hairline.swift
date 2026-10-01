import SwiftUI

struct Hairline: View {
    var isFocused: Bool = false
    var isError: Bool = false

    var body: some View {
        Rectangle()
            .fill(color)
            .frame(height: EmmSpacing.hairline)
            .accessibilityHidden(true)
    }

    private var color: Color {
        if isError { return EmmColors.danger }
        return isFocused ? EmmColors.borderFocus : EmmColors.border
    }
}
