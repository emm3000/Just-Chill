package com.emm.buildlogic.internal

import org.gradle.api.Project

private const val GIT_VARIABLE_PREFIX: String = "GIT_"

// Read at configuration time on purpose: a resolved providers.exec is a configuration-cache input,
// so a new commit or tag invalidates the cached entry.
internal fun Project.gitOutput(vararg arguments: String): String? = runCatching {
    providers.exec {
        commandLine(listOf("git") + arguments)
        setEnvironment(environment.filterKeys { !it.startsWith(GIT_VARIABLE_PREFIX) })
    }.standardOutput.asText.get().trim()
}.getOrNull()
