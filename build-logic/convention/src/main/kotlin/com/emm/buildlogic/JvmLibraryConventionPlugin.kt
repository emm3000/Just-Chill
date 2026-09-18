package com.emm.buildlogic

import com.emm.buildlogic.internal.BuildConventions
import com.emm.buildlogic.internal.configureKotlin
import com.emm.buildlogic.internal.gateOn
import com.emm.buildlogic.internal.libs
import com.emm.buildlogic.internal.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class JvmLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        apply<QualityGateConventionPlugin>()

        configureKotlin(BuildConventions.COROUTINES_OPT_INS)

        dependencies {
            add("testImplementation", kotlin("test"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
            add("testImplementation", libs.library("mockk"))
        }

        gateOn("test")
    }
}
