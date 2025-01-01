package dev.sculk.catalyst.tasks

import dev.sculk.catalyst.util.absolutePath
import  dev.sculk.catalyst.util.git
import dev.sculk.catalyst.util.path
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import kotlin.io.path.useDirectoryEntries

abstract class ApplyFeaturePatches : BaseTask() {
    @get:OutputDirectory
    abstract val output: DirectoryProperty
    
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputDirectory
    abstract val patchDir: DirectoryProperty

    override fun run() {
        git("reset", "--hard", "file")
        git("gc", silent = true)
        
        git("am", "--abort", silent = true)
        val patches = patchDir.path.useDirectoryEntries("*.patch") { it.toMutableList() }
        if (patches.isEmpty()) {
            logger.lifecycle("No patches to apply")
            return
        }
        
        patches.sort()
        try {
            patches.forEach { 
                git("am", "--3way", "--ignore-whitespace", it.absolutePath)
            }
        } catch (ex: RuntimeException) {
            logger.error("*** A feature patch didn't apply correctly.")
            logger.error("*** Please review the above details, finish the apply, and then")
            logger.error("*** commit your changes and run `gradlew rebuildFeaturePatches`")
            throw RuntimeException("Patch failed to apply")
        }
        
        logger.lifecycle("${patches.size} feature patches have been applied cleanly")
        git("tag", "-d", "feature", silent = true)
        git("tag", "feature", silent = true)
    }
    
    private fun git(vararg args: String, silent: Boolean = false) = git { 
        workingDir(output)
        args(*args)
        silent(silent)
        silentErr(silent)
    }
}
