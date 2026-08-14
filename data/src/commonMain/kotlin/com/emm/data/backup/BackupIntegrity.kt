package com.emm.data.backup

import okio.ByteString.Companion.toByteString

/**
 * SHA-256 of [bytes], lowercase hex, 64 characters.
 *
 * **It takes bytes and not a `String`, and that is the whole contract.** The snapshot travels to
 * storage as a byte sequence, and the read-back verification has to hash the identical sequence or
 * the comparison proves nothing about what was stored. A `sha256(json: String)` that encoded
 * internally would look interchangeable at every call site while quietly inviting one of them to
 * hash a re-encoding of the file instead of the file — same characters, different bytes, different
 * digest, and a mismatch indistinguishable from a corrupted upload. Encoding is the caller's
 * decision precisely so it happens once, before the bytes are both hashed and sent.
 *
 * Lowercase hex is stated here rather than left to the caller because the digest is written into a
 * manifest that is compared as text: `hex()` is okio's, it is lowercase, and
 * [BackupManifestDto.payloadSha256] is documented against this.
 *
 * okio rather than an `expect/actual` over `MessageDigest` / CommonCrypto — the fallback ADR 009
 * Phase 2b named — because `ByteString.sha256()` compiles from commonMain for both targets and needs
 * no provider setup. Two platform files avoided; see the catalog note on why okio is declared.
 */
internal fun sha256Hex(bytes: ByteArray): String = bytes.toByteString().sha256().hex()
