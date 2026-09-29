@preconcurrency import JustChillKit
import SwiftUI

extension MoreScreen {
    struct Actions {
        let onOpenCategories: () -> Void
        let onOpenLoans: () -> Void
        let onOpenManifesto: () -> Void
        let onOpenPrivacy: () -> Void
        let onPickBackup: () -> Void
        let onSignIn: () -> Void
    }

    struct Content: View {
        let state: ProfileUiState
        let send: Send
        let actions: Actions

        var body: some View {
            ScrollView {
                VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                    Text("Más")
                        .emmTextStyle(EmmType.titleL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s16, alignment: .leading)
                        .padding(.horizontal, EmmSpacing.s6)
                    if state.isSignedIn {
                        AccountSection(state: state, send: send)
                    }
                    destinations
                    data
                    app
                }
                .padding(.bottom, EmmSpacing.s6)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .alert("¿Reemplazar todo con el respaldo?", isPresented: isImportDialogPresented) {
                Button("Cancelar", role: .cancel) { send(ProfileIntentDialogDismissed.shared) }
                Button("Reemplazar", role: .destructive) {
                    send(ProfileIntentDialogDismissed.shared)
                    actions.onPickBackup()
                }
            } message: {
                Text(state.importWarning)
            }
        }

        private var destinations: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                SectionHeader(text: "Registro")
                Row(
                    symbol: "square.grid.2x2",
                    label: "Categorías",
                    meta: state.categoriesLabel,
                    isNavigable: true,
                    action: actions.onOpenCategories
                )
                Row(symbol: "person.2", label: "Préstamos", isNavigable: true, action: actions.onOpenLoans)
            }
        }

        private var data: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                SectionHeader(text: "Datos")
                Row(
                    symbol: "square.and.arrow.down",
                    label: "Exportar mi data",
                    meta: state.op == .exporting ? "Preparando…" : state.lastExportLabel,
                    isEnabled: state.op == .none,
                    isBusy: state.op == .exporting,
                    action: { send(ProfileIntentExportClicked.shared) }
                )
                Row(
                    symbol: "square.and.arrow.up",
                    label: "Importar respaldo",
                    meta: state.op == .importing ? "Importando…" : "Reemplaza todo lo que hay",
                    isEnabled: state.op == .none,
                    isBusy: state.op == .importing,
                    action: { send(ProfileIntentImportClicked.shared) }
                )
                if state.showsSignInRow {
                    Row(
                        symbol: "person.crop.circle",
                        label: "Iniciar sesión",
                        meta: "Respalda tus datos en la nube",
                        isNavigable: true,
                        action: actions.onSignIn
                    )
                }
                if !state.isCloudBackupAvailable {
                    LocalOnlyNote()
                }
            }
        }

        private var app: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                SectionHeader(text: "App")
                Row(
                    symbol: "info.circle",
                    label: "Acerca de JustChill",
                    meta: "El manifiesto · v\(appVersion)",
                    isNavigable: true,
                    action: actions.onOpenManifesto
                )
                Row(
                    symbol: "checkmark.shield",
                    label: "Privacidad",
                    meta: "100 % local, sin cuenta",
                    isNavigable: true,
                    action: actions.onOpenPrivacy
                )
            }
        }

        private var appVersion: String {
            Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
        }

        private var isImportDialogPresented: Binding<Bool> {
            Binding(get: { state.dialog == .import }, set: { _ in })
        }
    }

    struct SectionHeader: View {
        let text: String

        var body: some View {
            Text(text.uppercased())
                .emmTextStyle(EmmType.eyebrow)
                .foregroundStyle(EmmColors.textTertiary)
                .accessibilityAddTraits(.isHeader)
                .padding(.horizontal, EmmSpacing.s6)
                .padding(.top, EmmSpacing.s6)
                .padding(.bottom, EmmSpacing.s1)
        }
    }

    struct Row: View {
        let symbol: String
        let label: String
        var meta: String = ""
        var isNavigable: Bool = false
        var isEnabled: Bool = true
        var isBusy: Bool = false
        var pressedGround: Color = EmmColors.surface1
        var action: (() -> Void)?

        @Environment(\.dynamicTypeSize) private var dynamicTypeSize: DynamicTypeSize

        var body: some View {
            if let action {
                Button(action: action) { content }
                    .buttonStyle(RowPressStyle(pressedGround: pressedGround))
                    .disabled(!isEnabled)
            } else {
                content
                    .accessibilityElement(children: .combine)
            }
        }

        private var content: some View {
            HStack(alignment: dynamicTypeSize.isAccessibilitySize ? .top : .center, spacing: EmmSpacing.s3) {
                tile
                VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                    Text(label)
                        .emmTextStyle(EmmType.titleM)
                        .foregroundStyle(isActive ? EmmColors.textPrimary : EmmColors.textTertiary)
                    if !meta.isEmpty {
                        Text(meta)
                            .emmTextStyle(EmmType.bodyM)
                            .foregroundStyle(isActive ? EmmColors.textTertiary : EmmColors.textDisabled)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if isNavigable {
                    chevron
                }
            }
            .padding(.horizontal, EmmSpacing.s6)
            .padding(.vertical, EmmSpacing.s3)
            .frame(minHeight: EmmSpacing.s12)
            .contentShape(Rectangle())
        }

        private var isActive: Bool {
            isEnabled || isBusy
        }

        private var tile: some View {
            Image(systemName: symbol)
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                .foregroundStyle(isActive ? EmmColors.textSecondary : EmmColors.textTertiary)
                .frame(width: EmmSpacing.s10, height: EmmSpacing.s10)
                .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .accessibilityHidden(true)
        }

        private var chevron: some View {
            Image(systemName: "chevron.right")
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                .foregroundStyle(EmmColors.textTertiary)
                .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                .accessibilityHidden(true)
        }
    }

    struct RowPressStyle: ButtonStyle {
        let pressedGround: Color

        func makeBody(configuration: Configuration) -> some View {
            configuration.label
                .background(configuration.isPressed ? pressedGround : Color.clear)
        }
    }

    struct LocalOnlyNote: View {
        var body: some View {
            Text("Nada sale de tu teléfono. Si lo pierdes o lo cambias sin exportar, tu data se va con él.")
                .emmTextStyle(EmmType.bodyM)
                .foregroundStyle(EmmColors.textSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, EmmSpacing.s4)
                .padding(.vertical, EmmSpacing.s3)
                .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .padding(.horizontal, EmmSpacing.s6)
                .padding(.top, EmmSpacing.s2)
        }
    }
}
