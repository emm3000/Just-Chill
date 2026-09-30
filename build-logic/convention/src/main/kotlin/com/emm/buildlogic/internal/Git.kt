package com.emm.buildlogic.internal

import org.gradle.api.Project
import org.gradle.process.ExecOutput

private const val GIT_VARIABLE_PREFIX: String = "GIT_"
private const val SUCCESS_EXIT_VALUE: Int = 0

// Read at configuration time on purpose: a resolved providers.exec is a configuration-cache input,
// so a new commit or tag invalidates the cached entry. The exit value is read, never thrown: a
// failed exec is a configuration-cache problem that fails the build even when caught.
internal fun Project.gitOutput(vararg arguments: String): String? {
    val output: ExecOutput = providers.exec {
        commandLine(listOf("git") + arguments)
        setEnvironment(environment.filterKeys { !it.startsWith(GIT_VARIABLE_PREFIX) })
        isIgnoreExitValue = true
    }
    val standardOutput: String = output.standardOutput.asText.get().trim()
    return standardOutput.takeIf { output.result.get().exitValue == SUCCESS_EXIT_VALUE }
}
