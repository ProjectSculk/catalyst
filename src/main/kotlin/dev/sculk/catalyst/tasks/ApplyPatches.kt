package dev.sculk.catalyst.tasks

import codechicken.diffpatch.cli.PatchOperation
import codechicken.diffpatch.match.FuzzyLineMatcher
import codechicken.diffpatch.util.LoggingOutputStream
import dev.sculk.catalyst.util.file
import dev.sculk.catalyst.util.path
import org.eclipse.jgit.api.Git
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import java.io.PrintStream

abstract class ApplyPatches : BaseTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:InputDirectory
    abstract val patches: DirectoryProperty
    
    @get:Internal
    abstract val rejects: DirectoryProperty
    
    @get:Input
    abstract val fuzz: Property<Float>
    
    override fun setup() {
        fuzz.convention(FuzzyLineMatcher.DEFAULT_MIN_MATCH_SCORE)
    }
    
    override fun run() {
        val printStream = PrintStream(LoggingOutputStream(logger, LogLevel.LIFECYCLE))
        val result = PatchOperation.builder()
            .logTo(printStream)
            .basePath(outputDir.path)
            .patchesPath(patches.path)
            .outputPath(outputDir.path)
            .rejectsPath(rejects.path)
            .level(codechicken.diffpatch.util.LogLevel.INFO)
            .minFuzz(fuzz.get())
            .summary(false)
            .lineEnding("\n")
            .ignorePrefix(".git")
            .build()
            .operate()

        val git = Git.open(outputDir.file)
        commitAndTag(git, "patched", "apply patches")
        git.close()
        
        if (result.exit != 0) {
            val total = result.summary.failedMatches + result.summary.exactMatches +
                result.summary.accessMatches + result.summary.offsetMatches + result.summary.fuzzyMatches
            throw RuntimeException("Failed to apply ${result.summary.failedMatches}/$total hunks") 
        }
        
        logger.lifecycle("Successfully applied all ${result.summary.changedFiles} patches")
    }
}
