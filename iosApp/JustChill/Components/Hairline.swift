import SwiftUI

struct Hairline: View {
    var body: some View {
        Rectangle()
            .fill(EmmColors.border)
            .frame(height: EmmSpacing.hairline)
            .accessibilityHidden(true)
    }
}
