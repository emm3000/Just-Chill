package com.emm.buildlogic.internal

import org.gradle.api.Project

// Read at configuration time on purpose: a resolved providers.exec is a configuration-cache input,
// so a new commit or tag invalidates the cached entry.
internal fun Project.gitOutput(vararg arguments: String): String? = runCatching {
    providers.exec { commandLine(listOf("git") + arguments) }.standardOutput.asText.get().trim()
}.getOrNull()
