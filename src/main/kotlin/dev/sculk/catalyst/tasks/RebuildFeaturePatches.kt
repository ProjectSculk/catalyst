package dev.sculk.catalyst.tasks

import dev.sculk.catalyst.util.absolutePath
import dev.sculk.catalyst.util.git
import dev.sculk.catalyst.util.path
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "should always run")
abstract class RebuildFeaturePatches : BaseTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val patchDir: DirectoryProperty

    override fun run() {
        git(
            "format-patch",
            "--diff-algorithm=myers",
            "--zero-commit",
            "--full-index",
            "--no-signature",
            "--no-stat",
            "-N",
            "-o", patchDir.path.absolutePath,
            "file"
        )
        git("tag", "-d", "feature")
        git("tag", "feature")
    }

    private fun git(vararg args: String, silent: Boolean = false) = git {
        workingDir(inputDir)
        args(*args)
        silent(silent)
        silentErr(silent)
    }
}
