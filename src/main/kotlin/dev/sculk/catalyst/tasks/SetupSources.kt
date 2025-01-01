package dev.sculk.catalyst.tasks

import codechicken.diffpatch.cli.PatchOperation
import codechicken.diffpatch.util.LogLevel
import codechicken.diffpatch.util.archiver.ArchiveFormat
import dev.sculk.catalyst.util.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.PersonIdent
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import java.nio.file.Path
import java.util.function.Predicate
import javax.inject.Inject
import kotlin.io.path.*

abstract class SetupSources : BaseTask() {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    abstract val devImports: RegularFileProperty
    
    @get:Optional
    @get:InputDirectory
    abstract val patchesDir: DirectoryProperty
    
    @get:Optional
    @get:Classpath
    abstract val mache: ConfigurableFileCollection
    
    @get:Optional
    @get:InputFile
    abstract val atFile: RegularFileProperty
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:Internal
    abstract val filter: Property<Predicate<Path>>
    
    @get:Internal
    abstract val serverProject: Property<Project>
    
    @get:OutputDirectory
    abstract val base: DirectoryProperty

    @get:Classpath
    abstract val jst: ConfigurableFileCollection

    @get:CompileClasspath
    abstract val minecraftLibraries: ConfigurableFileCollection
    
    @get:Inject
    abstract val exec: ExecOperations
    
    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        val output = outputDir.path
        
        val git: Git
        if (output.resolve(".git/HEAD").isRegularFile()) {
            git = Git.open(output.toFile())
            git.reset().setRef("ROOT").setMode(ResetCommand.ResetType.HARD).call()
        } else {
            output.createDirectories()
            
            git = Git.init()
                .setDirectory(output.toFile())
                .setInitialBranch("main")
                .call()
            
            val rootIdent = PersonIdent("ROOT", "auto@mated.null")
            git.commit().setMessage("ROOT").setAllowEmpty(true).setAuthor(rootIdent).setSign(false).call()
            git.tagDelete().setTags("ROOT").call()
            git.tag().setName("ROOT").setTagger(rootIdent).setSigned(false).call()
        }
        
        inputFile.path.openFs().use { fs ->
            fs.walkSequence()
                .filter(filter.get()::test)
                .forEach { 
                    val target = output.resolve(it.toString().substring(1))
                    target.parent.createDirectories()
                    if (it.extension == "nbt") {
                        it.copyTo(target, overwrite = true)
                    } else {
                        var content = it.readText()
                        if (!content.endsWith("\n")) {
                            content += "\n"
                        }
                        target.writeText(content)
                    }
                }
        }
        
        commitAndTag(git, "Vanilla")
        
        if (!mache.isEmpty) {
            val result = PatchOperation.builder()
                .basePath(output)
                .outputPath(output)
                .patchesPath(mache.singleFile.toPath(), ArchiveFormat.ZIP)
                .patchesPrefix("patches")
                .level(LogLevel.OFF)
                .ignorePrefix(".git")
                .build()
                .operate()
            
            commitAndTag(git, "Mache")
            
            if (result.exit != 0) {
                throw RuntimeException("${result.summary.failedMatches} mache patches failed to apply")
            }
        }
        
        if (atFile.isPresent) {
            applySourceATs(
                exec,
                output,
                output,
                atFile.path,
                temporaryDir.toPath()
            )
            commitAndTag(git, "AT", "sculk at")
        }
        
        if (devImports.isPresent && patchesDir.isPresent) {
            McDev.importLibrarySources(
                serverProject.get(),
                devImports.path,
                patchesDir.path
                    .walk()
                    .filter { it.extension == "patch" }
                    .toList(),
                output
            )
            commitAndTag(git, "MCDev", "library imports")
        }
        
        val basePath = base.path
        if (basePath.notExists()) {
            basePath.deleteRecursively()
            basePath.createDirectories()
            output.copyToRecursively(basePath, overwrite = true, followLinks = false)
        }
        
        git.close()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun applySourceATs(
        exec: ExecOperations,
        input: Path,
        output: Path,
        at: Path,
        workDir: Path
    ) {
        workDir.deleteRecursively()
        workDir.createDirectories()

        val logFile = workDir.resolve("log.txt")
        logFile.useOutput { log ->
            exec.javaexec {
                standardOutput = log
                errorOutput = log

                classpath(jst)

                maxHeapSize = "1G"
                args = jstArgs(input, output, at)
            }
        }
    }

    private fun jstArgs(
        input: Path,
        output: Path,
        at: Path
    ): List<String> {
        return listOf(
            "--in-format=FOLDER",
            "--out-format=FOLDER",
            "--enable-accesstransformers",
            "--access-transformer=$at",
            "--access-transformer-inherit-method=true",
            "--hidden-prefix=.git",
            *minecraftLibraries.files.map { "--classpath=${it.absolutePath}" }.toTypedArray(),
            input.absolutePath,
            output.absolutePath
        )
    }
}


fun commitAndTag(git: Git, name: String, message: String = name) {
    val vanillaIdent = PersonIdent(name, "auto@mated.null")

    git.add().addFilepattern(".").call()
    git.add().addFilepattern(".").setUpdate(true).call()
    git.commit()
        .setMessage(message)
        .setAuthor(vanillaIdent)
        .setSign(false)
        .setAllowEmpty(true)
        .call()
    git.tagDelete().setTags(name).call()
    git.tag().setName(name).setTagger(vanillaIdent).setSigned(false).call()
}
