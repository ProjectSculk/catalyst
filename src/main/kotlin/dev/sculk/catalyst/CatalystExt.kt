package dev.sculk.catalyst

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class CatalystExt(objects: ObjectFactory) {
    val serverProject: Property<Project> = objects.property()
    
    val accessTransformers: RegularFileProperty = objects.fileProperty()
    val devImports: RegularFileProperty = objects.fileProperty()
}
