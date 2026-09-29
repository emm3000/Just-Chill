import SwiftUI
import UIKit
import UniformTypeIdentifiers

extension MoreScreen {
    struct BackupDocument: FileDocument {
        static let readableContentTypes: [UTType] = [.json]

        let json: String

        init(json: String) {
            self.json = json
        }

        init(configuration: ReadConfiguration) throws {
            guard let data = configuration.file.regularFileContents else {
                throw CocoaError(.fileReadCorruptFile)
            }
            json = String(decoding: data, as: UTF8.self)
        }

        func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
            FileWrapper(regularFileWithContents: Data(json.utf8))
        }

        static func suggestedFilename() -> String {
            "justchill-backup-" + Date.now.ISO8601Format(.iso8601Date(timeZone: .current)) + ".json"
        }

        static func read(from url: URL) -> String? {
            let isScoped = url.startAccessingSecurityScopedResource()
            defer {
                if isScoped { url.stopAccessingSecurityScopedResource() }
            }
            return try? String(contentsOf: url, encoding: .utf8)
        }
    }

    struct SharedCsv: Identifiable {
        let url: URL

        var id: URL { url }

        static func write(fileName: String, content: String) throws -> SharedCsv {
            let directory = FileManager.default.temporaryDirectory.appending(
                path: "exports", directoryHint: .isDirectory)
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            let file = directory.appending(path: fileName, directoryHint: .notDirectory)
            try content.write(to: file, atomically: true, encoding: .utf8)
            return SharedCsv(url: file)
        }
    }

    struct ShareSheet: UIViewControllerRepresentable {
        let url: URL
        let onFinish: (_ isFailed: Bool) -> Void

        func makeUIViewController(context: Context) -> UIActivityViewController {
            let controller = UIActivityViewController(activityItems: [url], applicationActivities: nil)
            controller.completionWithItemsHandler = { _, _, _, error in
                MainActor.assumeIsolated { onFinish(error != nil) }
            }
            return controller
        }

        func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
    }
}
