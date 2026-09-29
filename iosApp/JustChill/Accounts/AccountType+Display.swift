@preconcurrency import JustChillKit

extension AccountType {
    var symbolName: String {
        switch self {
        case .bank: "building.columns"
        case .cash: "banknote"
        case .creditCard: "creditcard"
        case .investment: "chart.line.uptrend.xyaxis"
        case .wallet: "wallet.bifold"
        }
    }

    var label: String {
        switch self {
        case .bank: "Banco"
        case .cash: "Efectivo"
        case .creditCard: "Tarjeta de crédito"
        case .investment: "Inversión"
        case .wallet: "Billetera digital"
        }
    }
}
