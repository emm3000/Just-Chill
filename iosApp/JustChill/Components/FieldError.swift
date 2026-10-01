import SwiftUI

struct FieldError: View {
    let message: String?

    var body: some View {
        if let message {
            Text(message)
                .emmTextStyle(EmmType.caption)
                .foregroundStyle(EmmColors.danger)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
