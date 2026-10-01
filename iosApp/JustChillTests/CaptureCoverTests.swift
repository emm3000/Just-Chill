import Testing

@testable import JustChill

struct CaptureCoverTests {
    @Test("refuses to present while the cover is still leaving")
    func refusesToPresentWhileLeaving() {
        var cover = CaptureCover()
        cover.present()
        cover.close()

        cover.present()

        #expect(cover.isShown == false)
    }

    @Test("presents again once the cover has finished leaving")
    func presentsAgainOnceDismissed() {
        var cover = CaptureCover()
        cover.present()
        cover.close()
        cover.finishLeaving()

        cover.present()

        #expect(cover.isShown)
    }
}
