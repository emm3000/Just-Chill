@preconcurrency import JustChillKit
import SwiftUI

extension MoreScreen {
    struct ExportSheet: View {
        let isIdle: Bool
        let send: Send

        var body: some View {
            ScrollView {
                VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                    SheetTitleBar(title: "Exportar", onClose: { send(ProfileIntentDialogDismissed.shared) })
                    Row(
                        symbol: "square.and.arrow.down",
                        label: "Respaldo completo",
                        meta: "Un archivo para restaurar todo",
                        isEnabled: isIdle,
                        pressedGround: EmmColors.surface2,
                        action: exportBackup
                    )
                    Row(
                        symbol: "calendar",
                        label: "Movimientos de este mes",
                        meta: "CSV para Excel",
                        isEnabled: isIdle,
                        pressedGround: EmmColors.surface2,
                        action: { send(ProfileIntentCsvExportRequested(scope: .currentMonth)) }
                    )
                    Row(
                        symbol: "tablecells",
                        label: "Todos los movimientos",
                        meta: "CSV con todo tu historial",
                        isEnabled: isIdle,
                        pressedGround: EmmColors.surface2,
                        action: { send(ProfileIntentCsvExportRequested(scope: .everything)) }
                    )
                }
                .padding(.bottom, EmmSpacing.s4)
            }
            .presentationBackground(EmmColors.surface1)
            .presentationDetents([.medium, .large])
        }

        private func exportBackup() {
            send(ProfileIntentDialogDismissed.shared)
            send(ProfileIntentExportRequested.shared)
        }
    }
}
