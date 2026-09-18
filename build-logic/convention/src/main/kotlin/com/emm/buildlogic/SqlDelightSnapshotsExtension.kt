package com.emm.buildlogic

import org.gradle.api.provider.Property

abstract class SqlDelightSnapshotsExtension {

    abstract val floor: Property<Int>
}
