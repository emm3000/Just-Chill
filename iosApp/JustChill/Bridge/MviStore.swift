@preconcurrency import JustChillKit
import Observation

@MainActor
@Observable
final class MviStore<State: AnyObject, Intent: AnyObject, Effect: AnyObject> {
    private(set) var state: State

    @ObservationIgnored private let handle: MviHandle<State, Intent, Effect>
    @ObservationIgnored private var effectHandler: ((Effect) -> Void)?

    init(_ handle: MviHandle<State, Intent, Effect>) {
        self.handle = handle
        state = handle.currentState
        handle.collectState { [weak self] state in
            MainActor.assumeIsolated { self?.state = state }
        }
    }

    func send(_ intent: Intent) {
        handle.send(intent: intent)
    }

    func onEffect(_ handler: @escaping (Effect) -> Void) {
        let isCollecting = effectHandler != nil
        effectHandler = handler
        guard !isCollecting else { return }
        handle.collectEffects { [weak self] effect in
            MainActor.assumeIsolated { self?.effectHandler?(effect) }
        }
    }

    deinit {
        handle.clear()
    }
}
