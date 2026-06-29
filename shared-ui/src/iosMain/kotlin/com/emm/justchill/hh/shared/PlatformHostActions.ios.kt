@file:OptIn(ExperimentalForeignApi::class)

package com.emm.justchill.hh.shared

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.profile.ProfileMessage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import kotlin.time.Clock

// iOS actual of the unified host's platform seams. Owns every UIKit/Foundation dependency so none of it
// leaks into commonMain, mirroring the Android SAF/Intent actual. Behavior matches Android:
//  - requestExport: write the JSON to a temp file, then present UIDocumentPickerViewController
//    (forExportingURLs) with a success snackbar on pick, error snackbar on write failure, nothing on
//    cancel — same ProfileMessage copy as Android.
//  - requestImport: present UIDocumentPickerViewController (forOpeningContentTypes = JSON), read the
//    picked (security-scoped) file and feed it to onImported. NO snackbar — the ProfileViewModel reports
//    import success/failure via ProfileEffect.Notify, exactly as on Android.
//  - onShareText: UIActivityViewController. iOS share sheets carry no chooser title, so Android's
//    "Compartir reporte" title is dropped (no equivalent).
//  - onOpenEmailApp: open "mailto:" via UIApplication.openURL; on failure show the SAME missing-app
//    snackbar as Android.
//  - supportsPrivacyPolicy / supportsBackup are flipped ON. The PrivacyPolicyScreen + route + entry
//    already live in commonMain, so the host now navigates there on onPrivacyClick (App-Store-required).
//
// Threading: every action runs from a Compose UI callback, which Compose invokes on the main thread, so
// the UIKit presentations below are already main-thread (R4). The only asynchrony is scope.launch for
// snackbars, which the root coroutine scope confines to the main dispatcher.

/**
 * Strong references to in-flight document-picker delegates.
 *
 * R1 — UIKit holds [UIDocumentPickerViewController.delegate] WEAKLY. A Kotlin/Native NSObject delegate
 * with no other strong owner is garbage-collected before the user finishes picking, so the picker fires
 * no callback and silently does nothing. We retain the delegate here from creation until its terminal
 * callback (didPick / wasCancelled), where it removes itself. Mutated only on the main thread (pickers
 * are created from Compose callbacks and UIKit delivers delegate callbacks on the main thread), so a
 * plain set needs no synchronization.
 */
private val retainedPickerDelegates: MutableSet<NSObject> = mutableSetOf()

/** Export picker delegate: success snackbar on pick, silent on cancel (Android parity). */
private class ExportPickerDelegate(
    private val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        retainedPickerDelegates.remove(this) // R1 — release self once the round-trip completes.
        scope.launch {
            snackbarHostState.showEmmSnackbar(
                message = ProfileMessage.ExportDone.toText(),
                tone = EmmSnackbarTone.Success,
            )
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        retainedPickerDelegates.remove(this) // R1 — release self on cancel; Android shows nothing either.
    }
}

/** Import picker delegate: read the security-scoped file and feed onImported. No snackbar (Android parity). */
private class ImportPickerDelegate(
    private val onImported: (String) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        retainedPickerDelegates.remove(this) // R1 — release self once the round-trip completes.
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        // R3 — a Files-app URL is security-scoped; the read fails without start/stop access.
        val accessGranted = url.startAccessingSecurityScopedResource()
        val text = NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null)
        if (accessGranted) url.stopAccessingSecurityScopedResource()
        // Mirror Android: silent on read failure; the VM surfaces import success/failure once onImported
        // runs (ProfileEffect.Notify). Nothing to show from here.
        if (text != null) onImported(text)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        retainedPickerDelegates.remove(this) // R1 — release self on cancel.
    }
}

/**
 * Topmost presented view controller, used to present every modal.
 *
 * R2 — presenting on a VC that already shows a modal throws/warns, so we start at the key window's root
 * and walk `.presentedViewController` to the top. `keyWindow` is deprecated since iOS 13 but still
 * resolves the active window on this single-scene, phone-first app; we fall back to the first window if
 * it is null. Returns null when no window/root exists; callers no-op gracefully (no crash).
 */
@Suppress("DEPRECATION")
private fun topmostViewController(): UIViewController? {
    val application = UIApplication.sharedApplication
    val root: UIViewController = application.keyWindow?.rootViewController
        ?: (application.windows.firstOrNull() as? UIWindow)?.rootViewController
        ?: return null
    var top: UIViewController = root
    while (true) {
        val presented = top.presentedViewController ?: break
        top = presented
    }
    return top
}

private fun presentShareSheet(text: String) {
    val presenter = topmostViewController() ?: return
    val activity = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    // iPad anchors the share sheet in a popover that crashes without a source; the app is phone-first, so
    // anchor to the presenter's view to stay safe on iPad too.
    activity.popoverPresentationController?.sourceView = presenter.view
    presenter.presentViewController(activity, animated = true, completion = null)
}

private fun openEmailApp(snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    val url = NSURL.URLWithString("mailto:") ?: return
    UIApplication.sharedApplication.openURL(
        url,
        options = emptyMap<Any?, Any?>(),
        completionHandler = { success ->
            if (!success) {
                scope.launch {
                    snackbarHostState.showEmmSnackbar(
                        message = "No encontramos una app de correo en tu teléfono.",
                        tone = EmmSnackbarTone.Error,
                    )
                }
            }
        },
    )
}

// CAST_NEVER_SUCCEEDS is a known Kotlin/Native frontend false positive for `String as NSString`: the
// two are unrelated to the type checker, but the K/N runtime bridges a Kotlin String to NSString on the
// cast, so writeToFile resolves on a real NSString instance. There is no cleaner first-class way to get
// an NSString receiver from a Kotlin String.
@Suppress("CAST_NEVER_SUCCEEDS")
private fun exportBackup(json: String, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    // ISO yyyy-MM-dd via kotlinx-datetime (matches Android's ISO_LOCAL_DATE; LocalDate.toString() is ISO).
    val filename = "justchill-backup-${Clock.System.todayIn(TimeZone.currentSystemDefault())}.json"
    val path = NSTemporaryDirectory() + filename
    val wrote = (json as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
    if (!wrote) {
        scope.launch {
            snackbarHostState.showEmmSnackbar(
                message = ProfileMessage.ExportFailed.toText(),
                tone = EmmSnackbarTone.Error,
            )
        }
        return
    }
    val presenter = topmostViewController() ?: return
    val picker = UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(path)))
    val delegate = ExportPickerDelegate(snackbarHostState, scope)
    retainedPickerDelegates.add(delegate) // R1 — keep alive until the delegate callback fires.
    picker.delegate = delegate
    presenter.presentViewController(picker, animated = true, completion = null)
}

private fun importBackup(onImported: (String) -> Unit) {
    val presenter = topmostViewController() ?: return
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON))
    val delegate = ImportPickerDelegate(onImported)
    retainedPickerDelegates.add(delegate) // R1 — keep alive until the delegate callback fires.
    picker.delegate = delegate
    presenter.presentViewController(picker, animated = true, completion = null)
}

@Composable
actual fun rememberPlatformHostActions(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onImported: (String) -> Unit,
): PlatformHostActions {
    // Latest onImported, mirroring the Android actual (the host re-creates this lambda each recomposition).
    val currentOnImported by rememberUpdatedState(onImported)
    return remember(snackbarHostState, scope) {
        IosHostActions(
            snackbarHostState = snackbarHostState,
            scope = scope,
            onImported = { text -> currentOnImported(text) },
        )
    }
}

private class IosHostActions(
    private val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope,
    private val onImported: (String) -> Unit,
) : PlatformHostActions {
    override val isDebug: Boolean = false
    override val showGoogleSignIn: Boolean = false
    override val supportsPrivacyPolicy: Boolean = true
    override val supportsBackup: Boolean = true

    override val onShareText: (String) -> Unit = { text -> presentShareSheet(text) }
    override val onOpenEmailApp: () -> Unit = { openEmailApp(snackbarHostState, scope) }
    override val requestExport: (String) -> Unit = { json -> exportBackup(json, snackbarHostState, scope) }
    override val requestImport: () -> Unit = { importBackup(onImported) }
}

actual val startTab: BottomBarRoute = HomeRoute
