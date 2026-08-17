package com.emm.data.backup

// Every generation here is derived from the live constant, never spelled. A fixture that said
// "backup-v3-" outright would keep passing the day BACKUP_SCHEMA_VERSION becomes 4 — while every
// v3 file already in a user's bucket went invisible, which is the regression these fixtures exist
// to catch.
internal fun String.asGeneration(generation: Int): String = replace("-v$BACKUP_SCHEMA_VERSION-", "-v$generation-")

internal fun String.asEarlierGeneration(): String = asGeneration(BACKUP_SCHEMA_VERSION - 1)
