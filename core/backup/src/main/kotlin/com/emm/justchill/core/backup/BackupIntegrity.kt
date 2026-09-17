package com.emm.justchill.core.backup

import okio.ByteString.Companion.toByteString

internal fun sha256Hex(bytes: ByteArray): String = bytes.toByteString().sha256().hex()
