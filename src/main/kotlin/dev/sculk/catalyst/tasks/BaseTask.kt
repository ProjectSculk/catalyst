package dev.sculk.catalyst.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class BaseTask : DefaultTask() {
    @get:Inject
    abstract val objects: ObjectFactory
    
    open fun setup() {}
    
    abstract fun run()
    
    init {
        this.setup()
    }
    
    @TaskAction
    fun execute() {
        this.run()
    }
}
