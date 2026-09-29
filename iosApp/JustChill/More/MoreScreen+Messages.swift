@preconcurrency import JustChillKit

extension MoreScreen {
    static func text(for message: any ProfileMessage) -> String? {
        switch onEnum(of: message) {
        case .exportDone: "Listo, tu data está guardada."
        case .exportFailed: "No pude exportar — capaz no hay espacio en tu celu?"
        case .csvExportFailed: "No pude exportar tus movimientos. Inténtalo de nuevo."
        case .importDone(let done): done.summary
        case .importFailed: "No pude importar el archivo — capaz está dañado."
        case .operationInProgress: "Espera a que termine la operación en curso."
        case .sessionClosed, .sessionClosedLocallyOnly, .accountDeleted, .backup: nil
        }
    }
}
