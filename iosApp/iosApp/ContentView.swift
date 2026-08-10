import SwiftUI
import JustChillKit

// S2 bootstrap smoke view (docs/swiftui/PLAN.md): proves the whole chain Swift -> JustChillKit ->
// Koin -> ViewModel -> SQLDelight -> SKIE AsyncSequence before any real screen exists. Replaced
// screen by screen from slice S3 on; the CMP host (ComposeView + MainViewController) is gone.
struct ContentView: View {
    private let viewModel = KoinIosKt.seeTransactionsViewModel()
    @State private var summary = "Loading transactions…"

    var body: some View {
        Text(summary)
            .padding()
            .task {
                // SKIE exposes StateFlow as an AsyncSequence; each emission is one state.
                for await state in viewModel.state {
                    let days = state.days.count
                    let income = state.incomeCount
                    let spend = state.spendCount
                    summary = "JustChillKit up — \(days) day groups, \(income) income / \(spend) spend"
                }
            }
    }
}
