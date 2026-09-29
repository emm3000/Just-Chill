import SwiftUI

struct CaptureScreen: View {
    let onClose: () -> Void

    var body: some View {
        NavigationStack {
            Text("Aún no puedes anotar movimientos")
                .navigationTitle("Anotar movimiento")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button(action: onClose) {
                            Image(systemName: "xmark")
                        }
                        .accessibilityLabel("Cerrar")
                    }
                }
        }
    }
}
