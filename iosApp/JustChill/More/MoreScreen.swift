@preconcurrency import JustChillKit
import SwiftUI
import UniformTypeIdentifiers

struct MoreScreen: View {
    typealias Store = MviStore<ProfileUiState, any ProfileIntent, any ProfileEffect>
    typealias Send = (any ProfileIntent) -> Void

    enum Destination: Hashable {
        case manifesto
        case privacy
        case categories
    }

    var onOpenLoans: () -> Void = {}

    @State private var store: Store?
    @State private var path: [Destination] = []
    @State private var notice: String?
    @State private var pendingExport: BackupDocument?
    @State private var pendingCsv: SharedCsv?
    @State private var isExportSheetOnScreen: Bool = false
    @State private var isImporterPresented: Bool = false
    @State private var pendingImportJson: String?

    var body: some View {
        NavigationStack(path: $path) {
            root
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: Destination.self, destination: destination)
        }
    }

    private var root: some View {
        Group {
            if let store {
                Content(
                    state: store.state,
                    send: { store.send($0) },
                    actions: Actions(
                        onOpenCategories: { path.append(.categories) },
                        onOpenLoans: onOpenLoans,
                        onOpenManifesto: { path.append(.manifesto) },
                        onOpenPrivacy: { path.append(.privacy) },
                        onPickBackup: { isImporterPresented = true }
                    )
                )
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveProfileHandle())
            newStore.onEffect(handle)
            store = newStore
        }
        .sheet(isPresented: isExportSheetPresented, onDismiss: { isExportSheetOnScreen = false }) {
            ExportSheet(isIdle: store?.state.op == ProfileOp.none, send: { store?.send($0) })
                .onAppear { isExportSheetOnScreen = true }
        }
        .fileExporter(
            isPresented: isExporterPresented,
            document: pendingExport,
            contentType: .json,
            defaultFilename: BackupDocument.suggestedFilename(),
            onCompletion: finishExport
        )
        .fileImporter(isPresented: $isImporterPresented, allowedContentTypes: [.json], onCompletion: readBackup)
        .sheet(item: presentableCsv) { csv in
            ShareSheet(url: csv.url, onFinish: finishShare)
                .presentationDetents([.medium, .large])
        }
        .alert("¿Reemplazar tu data?", isPresented: isImportConfirmationPresented) {
            Button("Cancelar", role: .cancel) { pendingImportJson = nil }
            Button("Reemplazar todo", role: .destructive, action: confirmImport)
        } message: {
            Text("Esto va a borrar todo lo que tengas hoy y poner lo del archivo.")
        }
        .alert(notice ?? "", isPresented: isNoticePresented) {
            Button("Aceptar") { notice = nil }
        }
    }

    @ViewBuilder
    private func destination(_ destination: Destination) -> some View {
        switch destination {
        case .manifesto:
            ManifestoScreen(isRevisit: true, onStart: { path.removeLast() })
                .toolbar(.hidden, for: .navigationBar)
                .toolbar(.hidden, for: .tabBar)
        case .privacy:
            PrivacyPolicy(onBack: { path.removeLast() })
                .toolbar(.hidden, for: .navigationBar)
                .toolbar(.hidden, for: .tabBar)
        case .categories:
            CategoriesScreen(onBack: { path.removeLast() })
                .toolbar(.hidden, for: .navigationBar)
                .toolbar(.hidden, for: .tabBar)
        }
    }

    private func handle(_ effect: any ProfileEffect) {
        switch onEnum(of: effect) {
        case .showError(let failure): notice = failure.error.toUserMessage()
        case .notify(let notify): notice = Self.text(for: notify.message)
        case .exportReady(let ready): pendingExport = BackupDocument(json: ready.json)
        case .csvReady(let csv): shareCsv(fileName: csv.fileName, content: csv.content)
        }
    }

    private func finishExport(_ result: Result<URL, any Error>) {
        pendingExport = nil
        switch result {
        case .success: store?.send(ProfileIntentExportFinished(saved: true))
        case .failure(CocoaError.userCancelled): break
        case .failure: store?.send(ProfileIntentExportFinished(saved: false))
        }
    }

    private func shareCsv(fileName: String, content: String) {
        do {
            pendingCsv = try SharedCsv.write(fileName: fileName, content: content)
        } catch {
            store?.send(ProfileIntentCsvShareFailed.shared)
        }
    }

    private func finishShare(isFailed: Bool) {
        discardCsv()
        if isFailed { store?.send(ProfileIntentCsvShareFailed.shared) }
    }

    private func discardCsv() {
        guard let csv = pendingCsv else { return }
        try? FileManager.default.removeItem(at: csv.url)
        pendingCsv = nil
    }

    private func readBackup(_ result: Result<URL, any Error>) {
        guard case .success(let url) = result, let json = BackupDocument.read(from: url) else { return }
        pendingImportJson = json
    }

    private func confirmImport() {
        guard let json = pendingImportJson else { return }
        pendingImportJson = nil
        store?.send(ProfileIntentImportJson(json: json))
    }

    private var isExportSheetPresented: Binding<Bool> {
        Binding(
            get: { store?.state.dialog == .export },
            set: { isPresented in
                if !isPresented { store?.send(ProfileIntentDialogDismissed.shared) }
            }
        )
    }

    private var isExporterPresented: Binding<Bool> {
        Binding(
            get: { pendingExport != nil && !isExportSheetOnScreen },
            set: { isPresented in
                if !isPresented { pendingExport = nil }
            }
        )
    }

    private var presentableCsv: Binding<SharedCsv?> {
        Binding(
            get: { isExportSheetOnScreen ? nil : pendingCsv },
            set: { csv in
                if csv == nil { discardCsv() }
            }
        )
    }

    private var isImportConfirmationPresented: Binding<Bool> {
        Binding(
            get: { pendingImportJson != nil },
            set: { isPresented in
                if !isPresented { pendingImportJson = nil }
            }
        )
    }

    private var isNoticePresented: Binding<Bool> {
        Binding(
            get: { notice != nil },
            set: { isPresented in
                if !isPresented { notice = nil }
            }
        )
    }
}
