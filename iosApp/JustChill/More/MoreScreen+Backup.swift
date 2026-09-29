@preconcurrency import JustChillKit
import SwiftUI

extension MoreScreen {
    struct BackupRows: View {
        let state: ProfileUiState
        let send: Send

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                if state.showsBackupDestinationDisclosure {
                    DestinationDisclosure(onAcknowledge: { send(ProfileIntentAcknowledgeBackupDestination.shared) })
                }
                Row(
                    symbol: "icloud.and.arrow.up",
                    label: "Respaldar ahora",
                    meta: state.op == .backingUp ? "Respaldando…" : "Sube una copia a la nube",
                    isNavigable: true,
                    isEnabled: state.op == .none,
                    isBusy: state.op == .backingUp,
                    action: { send(ProfileIntentBackUpNow.shared) }
                )
                Row(
                    symbol: "checkmark.icloud",
                    label: "Verificar respaldo",
                    meta: state.op == .verifyingBackup ? "Verificando…" : "Revisa que el último se pueda restaurar",
                    isNavigable: true,
                    isEnabled: state.op == .none,
                    isBusy: state.op == .verifyingBackup,
                    action: { send(ProfileIntentVerifyBackup.shared) }
                )
                Row(
                    symbol: "icloud",
                    label: "Último respaldo",
                    meta: state.backupRow.toMetaText(),
                    metaColor: statusColor
                )
            }
        }

        private var statusColor: Color? {
            switch state.backupRow.severity() {
            case .normal: nil
            case .warning: EmmColors.warning
            case .danger: EmmColors.danger
            }
        }
    }

    private struct DestinationDisclosure: View {
        let onAcknowledge: () -> Void

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                Text(BACKUP_DESTINATION_DISCLOSURE)
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, EmmSpacing.s6)
                    .padding(.top, EmmSpacing.s2)
                SaveButton(
                    label: BACKUP_DESTINATION_DISCLOSURE_ACTION,
                    isEnabled: true,
                    isSaving: false,
                    onSave: onAcknowledge
                )
            }
        }
    }
}
