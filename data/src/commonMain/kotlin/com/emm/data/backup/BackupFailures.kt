package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs one storage call and turns anything it throws into a [DomainException] whose message is
 * exactly [reason].
 *
 * This and [asBackupFailure] sit in a file of their own because they have **two** callers now — the
 * upload and the retention prune — and those are separate operations that fail for separate reasons.
 * What they share is not text but knowledge: which Supabase and Ktor types mean "refused", "no
 * session" and "the network is gone", and that a cancelled operation is the caller leaving rather
 * than a failure. Copying that table into a second file would be the kind of duplication
 * `docs/CODE_QUALITY.md` calls repeated knowledge — one copy would learn about a new exception type
 * and the other would not.
 *
 * The headline is NOT shared, deliberately: [reason] is a whole message, so a prune failure never
 * reads "Snapshot backup failed" and an upload failure never reads like a prune.
 *
 * `CancellationException` is rethrown before the generic catch, the same property Phase 0's sign-out
 * fix pinned: a cancelled operation is the caller going away, not a failure, and reporting it as one
 * would put a phantom outage in front of whoever is reading these messages.
 */
@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> storageCall(reason: String, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    throw e.asBackupFailure(reason)
}

/**
 * Supabase/Ktor throwable to [DomainException], keeping [reason] as the message.
 *
 * A [DomainException] passes through unchanged — [BackupObjectStore.ownedPrefix] already throws one
 * for a missing session and [BackupObjectStore.upload] one for a key that does not end in `.json`,
 * and rewrapping either would bury the failures here that are not transport problems under a message
 * about the step that merely noticed them.
 *
 * [RestException] carries the server's own status, and it is spelled into the message rather than
 * left in the cause because the two refusals this bucket is configured to produce are unreadable
 * without it: **413** is a payload over the 10 MiB ceiling and **415** is a content type the bucket
 * does not allow. Both are deliberate server-side refusals, not faults, and a message that omitted
 * the code would send a reader hunting for a network problem that is not there.
 */
internal fun Throwable.asBackupFailure(reason: String): DomainException = when (this) {
    is DomainException -> this

    is UnauthorizedRestException -> DomainException.Unauthorized(reason, this)

    // Storage is installed with requireValidSession = true, so an unresolved session throws instead
    // of silently downgrading to the anon key and collecting an RLS refusal that explains nothing.
    is SessionRequiredException -> DomainException.Unauthorized(reason, this)

    is RestException -> DomainException.Unknown(this, "$reason The server answered HTTP $statusCode: $error.")

    is HttpRequestException,
    is HttpRequestTimeoutException,
    -> DomainException.NetworkUnavailable(this, reason)

    else -> DomainException.Unknown(this, reason)
}
