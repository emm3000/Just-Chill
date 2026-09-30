import SwiftUI

enum LoansRoute: Hashable {
    case people
    case personLoans(String)
}

struct LoansFlow: View {
    let route: LoansRoute
    let onBack: () -> Void
    let onOpenPerson: (String) -> Void

    var body: some View {
        screen
            .toolbar(.hidden, for: .navigationBar)
            .toolbar(.hidden, for: .tabBar)
    }

    @ViewBuilder
    private var screen: some View {
        switch route {
        case .people:
            LoansScreen(onBack: onBack, onOpenPerson: onOpenPerson)
        case .personLoans(let personKey):
            PersonLoansScreen(personKey: personKey, onBack: onBack)
        }
    }
}
