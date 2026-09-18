package com.emm.buildlogic

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

internal const val GIT_VARIABLE_PREFIX: String = "GIT_"

internal fun leakedGitEnvironment(repository: File): Map<String, String> {
    val gitDirectory: File = File(repository, ".git")
    return mapOf(
        "GIT_DIR" to gitDirectory.absolutePath,
        "GIT_WORK_TREE" to repository.absolutePath,
        "GIT_INDEX_FILE" to File(gitDirectory, "index").absolutePath,
        "GIT_OBJECT_DIRECTORY" to File(gitDirectory, "objects").absolutePath,
        "GIT_COMMON_DIR" to gitDirectory.absolutePath,
    )
}

internal class ConventionPluginFixture(
    private val projectDirectory: File,
    private val ambientEnvironment: Map<String, String> = emptyMap(),
) {

    fun report(
        pluginIds: List<String>,
        androidConfiguration: String = "",
        arguments: List<String> = emptyList(),
        files: Map<String, String> = emptyMap(),
    ): Map<String, String> {
        files.forEach { (path, content) -> write(path, content) }
        writeSettings(listOf(":probe", ":core:domain", ":core:ui", ":core:testing"))
        writeLocalProperties()
        writeStubModule("core/domain")
        writeStubModule("core/ui")
        writeStubModule("core/testing")
        writeProbeModule(pluginIds, androidConfiguration)

        val result: BuildResult = runner(listOf(":probe:conventionReport") + arguments).build()

        return result.output
            .lineSequence()
            .filter { it.startsWith(REPORT_PREFIX) }
            .associate { it.removePrefix(REPORT_PREFIX).substringBefore('=') to it.substringAfter('=') }
    }

    fun check(
        task: String,
        modules: Map<String, String>,
        sources: Map<String, String> = emptyMap(),
    ): BuildResult {
        prepare(modules, sources)
        return runner(listOf(task)).build()
    }

    fun checkAndFail(
        task: String,
        modules: Map<String, String>,
        sources: Map<String, String> = emptyMap(),
    ): String {
        prepare(modules, sources)
        return runner(listOf(task)).buildAndFail().output
    }

    fun git(vararg arguments: String): String {
        val errorFile: File = File.createTempFile("convention-git", ".log")
        val process: Process = gitBuilder(arguments.toList()).redirectError(errorFile).start()
        val output: String = process.inputStream.bufferedReader().readText()
        val failed: Boolean = process.waitFor() != 0
        val errors: String = errorFile.readText()
        errorFile.delete()
        check(!failed) { "git ${arguments.joinToString(" ")} failed: $output$errors" }
        return output.trim()
    }

    fun gitEnvironment(): Map<String, String> = gitBuilder(emptyList()).environment()

    private fun gitBuilder(arguments: List<String>): ProcessBuilder {
        val builder: ProcessBuilder = ProcessBuilder(listOf("git") + GIT_IDENTITY + arguments)
            .directory(projectDirectory)
        builder.environment().putAll(ambientEnvironment)
        builder.environment().keys.removeAll { it.startsWith(GIT_VARIABLE_PREFIX) }
        return builder
    }

    private fun prepare(modules: Map<String, String>, sources: Map<String, String>) {
        sources.forEach { (path, content) -> write(path, content) }
        writeSettings(modules.keys.toList())
        writeLocalProperties()
        modules.forEach { (path, content) ->
            write("${path.removePrefix(":").replace(':', '/')}/build.gradle.kts", content)
        }
    }

    private fun runner(arguments: List<String>): GradleRunner {
        val runner: GradleRunner = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath()
            .withArguments(arguments + listOf("-g", gradleUserHome, "--stacktrace"))
        if (ambientEnvironment.isEmpty()) return runner
        return runner.withEnvironment(System.getenv() + ambientEnvironment)
    }

    private fun writeSettings(includes: List<String>) {
        val catalog: File = File(rootDirectory, "gradle/libs.versions.toml")
        write(
            "settings.gradle.kts",
            """
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
                versionCatalogs {
                    create("libs") {
                        from(files("${catalog.invariantSeparatorsPath}"))
                    }
                }
            }

            rootProject.name = "convention-fixture"
            ${includes.joinToString(separator = "\n            ") { """include("$it")""" }}
            """.trimIndent(),
        )
    }

    private fun writeLocalProperties() {
        val androidHome: String = System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: File(System.getProperty("user.home"), "Library/Android/sdk").absolutePath
        write("local.properties", "sdk.dir=${File(androidHome).invariantSeparatorsPath}")
    }

    private fun writeStubModule(path: String) {
        write("$path/build.gradle.kts", "")
    }

    private fun writeProbeModule(pluginIds: List<String>, androidConfiguration: String) {
        val content: String = buildString {
            appendLine("plugins {")
            pluginIds.forEach { appendLine("""    id("$it")""") }
            appendLine("}")
            appendLine()
            appendLine(androidConfiguration)
            appendLine()
            append(REPORT_TASK)
        }
        write("probe/build.gradle.kts", content)
    }

    private fun write(path: String, content: String) {
        val target: File = File(projectDirectory, path)
        target.parentFile.mkdirs()
        target.writeText(content)
    }

    private companion object {
        const val REPORT_PREFIX: String = "REPORT "

        val rootDirectory: File = File(System.getProperty("justchill.rootDir"))

        val GIT_IDENTITY: List<String> = listOf(
            "-c", "user.name=t",
            "-c", "user.email=t@t",
            "-c", "commit.gpgsign=false",
            "-c", "tag.gpgsign=false",
        )

        val gradleUserHome: String = File(System.getProperty("user.home"), ".gradle").absolutePath

        val REPORT_TASK: String = """
            tasks.register("conventionReport") {
                doLast {
                    val android = project.extensions.findByType(com.android.build.api.dsl.CommonExtension::class.java)
                    if (android != null) {
                        println("REPORT compileSdk=" + android.compileSdk)
                        println("REPORT minSdk=" + android.defaultConfig.minSdk)
                        println("REPORT sourceCompatibility=" + android.compileOptions.sourceCompatibility)
                        println("REPORT compose=" + android.buildFeatures.compose)
                        println("REPORT namespace=" + android.namespace)
                    }
                    val library = project.extensions.findByType(com.android.build.api.dsl.LibraryExtension::class.java)
                    if (library != null) {
                        println("REPORT testTargetSdk=" + library.testOptions.targetSdk)
                    }
                    val application = project.extensions.findByType(com.android.build.api.dsl.ApplicationExtension::class.java)
                    if (application != null) {
                        println("REPORT targetSdk=" + application.defaultConfig.targetSdk)
                        println("REPORT versionCode=" + application.defaultConfig.versionCode)
                        println("REPORT versionName=" + application.defaultConfig.versionName)
                        val release = application.buildTypes.getByName("release")
                        println("REPORT releaseMinify=" + release.isMinifyEnabled)
                        println("REPORT releaseShrink=" + release.isShrinkResources)
                        println("REPORT releaseProguard=" + release.proguardFiles.map { it.name }.joinToString(","))
                        println("REPORT releaseSigning=" + release.signingConfig?.name)
                        println("REPORT signingConfigs=" + application.signingConfigs.map { it.name }.sorted().joinToString(","))
                        application.signingConfigs.findByName("release")?.let { signing ->
                            println("REPORT releaseStoreFile=" + signing.storeFile?.relativeTo(project.projectDir)?.invariantSeparatorsPath)
                            println("REPORT releaseKeyAlias=" + signing.keyAlias)
                        }
                        val crashlytics = (release as ExtensionAware).extensions.findByName("firebaseCrashlytics")
                        if (crashlytics != null) {
                            println("REPORT mappingUpload=" + (crashlytics as com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension).mappingFileUploadEnabled)
                        }
                    }
                    val sqldelight = project.extensions.findByType(app.cash.sqldelight.gradle.SqlDelightExtension::class.java)
                    sqldelight?.databases?.forEach { database ->
                        println("REPORT database=" + listOf(database.name, database.packageName.get(), database.schemaOutputDirectory.get().asFile.relativeTo(project.projectDir).invariantSeparatorsPath, database.verifyMigrations.get()).joinToString(","))
                    }
                    val java = project.extensions.findByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
                    if (java != null) {
                        println("REPORT javaToolchain=" + java.toolchain.languageVersion.get())
                    }
                    val compilations = project.tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java)
                    println("REPORT jvmTarget=" + compilations.map { it.compilerOptions.jvmTarget.get().target }.distinct().sorted().joinToString(","))
                    println("REPORT optIn=" + compilations.flatMap { it.compilerOptions.optIn.get() }.distinct().sorted().joinToString(","))
                    val declared = project.configurations.flatMap { configuration -> configuration.dependencies.map { configuration.name to it } }
                    println("REPORT projectDependencies=" + declared.map { it.second }.filterIsInstance<org.gradle.api.artifacts.ProjectDependency>().map { it.path }.filter { it != project.path }.distinct().sorted().joinToString(","))
                    println("REPORT implementationDependencies=" + declared.filter { it.first == "implementation" }.map { it.second.name }.distinct().sorted().joinToString(","))
                    println("REPORT testDependencies=" + declared.filter { it.first == "testImplementation" }.map { it.second.name }.distinct().sorted().joinToString(","))
                    println("REPORT plugins=" + listOf("justchill.quality.gate").filter { project.pluginManager.hasPlugin(it) }.joinToString(","))
                    val gate = project.tasks.findByName("qualityGate")
                    if (gate != null) {
                        println("REPORT gatedTests=" + gate.dependsOn.filterIsInstance<String>().sorted().joinToString(","))
                        if (project.providers.gradleProperty("justchill.reportGateTasks").isPresent) {
                            println("REPORT gateTasks=" + gate.taskDependencies.getDependencies(gate).map { it.name }.sorted().joinToString(","))
                        }
                    }
                }
            }
        """.trimIndent()
    }
}
