package com.emm.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CheckLazyListKeysTask : DefaultTask() {

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val files: List<File> = sources.files.filter { it.isFile }
        val declarations: Declarations = declarations(files)
        val offenders: List<String> = files
            .flatMap { file -> offenders(file, declarations) }
            .sorted()

        if (offenders.isNotEmpty()) {
            throw GradleException(
                offenders.joinToString(separator = "\n", prefix = "$FAILURE_HEADER\n"),
            )
        }

        val reportFile: File = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(files.size.toString())
    }

    private fun declarations(files: List<File>): Declarations {
        val byOwner: MutableMap<String, String> = mutableMapOf()
        val byName: MutableMap<String, MutableSet<String>> = mutableMapOf()
        val elements: MutableMap<String, MutableSet<String>> = mutableMapOf()
        files.forEach { file ->
            var owner: String = UNKNOWN_OWNER
            file.useLines { lines ->
                lines.forEach { line ->
                    DECLARATION.find(line)?.let { owner = it.groupValues[1] }
                    PROPERTY.findAll(line).forEach { match ->
                        val property: String = match.groupValues[1]
                        val type: String = match.groupValues[3].substringAfterLast('.')
                        byOwner.putIfAbsent("$owner$OWNER_SEPARATOR$property", type)
                        byName.getOrPut(property) { mutableSetOf() }.add(type)
                        ELEMENT.find(match.groupValues[2])?.let { element ->
                            elements.getOrPut(property) { mutableSetOf() }
                                .add(element.groupValues[1].substringAfterLast('.'))
                        }
                    }
                }
            }
        }
        return Declarations(byOwner, byName, elements)
    }

    private fun offenders(file: File, declarations: Declarations): List<String> =
        file.useLines { lines ->
            lines.mapIndexedNotNull { index, line ->
                val key: Key = key(line) ?: return@mapIndexedNotNull null
                val resolution: Resolution = declarations.resolve(key) ?: return@mapIndexedNotNull null
                if (resolution.type in PRIMITIVES) return@mapIndexedNotNull null
                "${file.invariantSeparatorsPath}:${index + 1}: " +
                    "${resolution.owner}.${key.property} is ${resolution.type}"
            }.toList()
        }

    private fun key(line: String): Key? {
        val reference: MatchResult? = REFERENCE_KEY.find(line)
        if (reference != null) {
            val property: String = reference.groupValues[2]
            if (property == UNDERLYING_VALUE) return null
            return Key(property = property, owner = reference.groupValues[1].substringAfterLast('.'))
        }
        val lambda: MatchResult = LAMBDA_KEY.find(line) ?: return null
        val property: String = lambda.groupValues[1].substringAfterLast('.')
        if (property == UNDERLYING_VALUE) return null
        return Key(property = property, collection = ITEMS.find(line)?.groupValues?.get(1))
    }

    private data class Key(
        val property: String,
        val owner: String? = null,
        val collection: String? = null,
    )

    private data class Resolution(val owner: String, val type: String)

    private class Declarations(
        private val byOwner: Map<String, String>,
        private val byName: Map<String, Set<String>>,
        private val elements: Map<String, Set<String>>,
    ) {

        fun resolve(key: Key): Resolution? {
            val owner: String? = key.owner ?: elementOf(key.collection)
            if (owner != null) {
                val declared: String = byOwner["$owner$OWNER_SEPARATOR${key.property}"] ?: return null
                return Resolution(owner = owner, type = declared)
            }
            val candidate: String = byName[key.property].orEmpty().singleOrNull() ?: return null
            return Resolution(owner = UNKNOWN_OWNER, type = candidate)
        }

        private fun elementOf(collection: String?): String? =
            elements[collection?.substringAfterLast('.')]?.singleOrNull()
    }

    private companion object {
        const val UNKNOWN_OWNER: String = "?"
        const val OWNER_SEPARATOR: String = "#"
        const val UNDERLYING_VALUE: String = "value"

        val DECLARATION: Regex = Regex("""\b(?:class|object|interface)\s+([A-Z]\w*)""")
        val PROPERTY: Regex = Regex("""\bva[lr]\s+([A-Za-z_]\w*)\s*:\s*(([A-Za-z_][\w.]*)[^,)=\n]*)""")
        val ELEMENT: Regex = Regex("""^(?:[A-Za-z_][\w.]*)?List<([A-Za-z_][\w.]*)>""")
        val LAMBDA_KEY: Regex = Regex("""\bkey\s*=\s*\{\s*it((?:\.[A-Za-z_]\w*)+)\s*}""")
        val REFERENCE_KEY: Regex = Regex("""\bkey\s*=\s*([A-Za-z_][\w.]*)::([A-Za-z_]\w*)""")
        val ITEMS: Regex = Regex("""\bitems(?:Indexed)?\s*\(\s*([A-Za-z_][\w.]*)""")

        const val FAILURE_HEADER: String =
            "A LazyList key must survive bundle serialization: pass the id's underlying " +
                "primitive, e.g. `it.accountId.value`."

        val PRIMITIVES: Set<String> = setOf(
            "String",
            "Long",
            "Int",
            "Short",
            "Byte",
            "Char",
            "Boolean",
            "Double",
            "Float",
        )
    }
}
