package dev.sculk.catalyst.tasks

import dev.sculk.catalyst.util.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject
import kotlin.io.path.name

@CacheableTask
abstract class RemapJar : BaseTask() {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val inputJar: RegularFileProperty
    
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val mappings: RegularFileProperty
    
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val paramMappings: ConfigurableFileCollection
    
    @get:Input
    abstract val remapperArgs: ListProperty<String>
    
    @get:OutputFile
    abstract val outputJar: RegularFileProperty
    
    @get:CompileClasspath
    abstract val minecraftLibraries: ConfigurableFileCollection
    
    @get:Classpath
    abstract val codebook: ConfigurableFileCollection
    
    @get:Classpath
    abstract val remapper: ConfigurableFileCollection
    
    @get:Inject
    abstract val exec: ExecOperations

    override fun run() {
        val out = outputJar.path.cleanFile()
        val logFile = out.resolveSibling("${out.name}.log")
        logFile.useOutput { log ->
            exec.javaexec {
                standardOutput = log
                errorOutput = log
                
                classpath(codebook)
                
                remapperArgs.get().forEach { arg ->
                    args(arg
                        .replace(Regex("\\{tempDir}")) { project.buildDirectory.resolve(".tmp_codebook").absolutePath }
                        .replace(Regex("\\{remapperFile}")) { remapper.singleFile.absolutePath }
                        .replace(Regex("\\{mappingsFile}")) { mappings.path.absolutePath }
                        .replace(Regex("\\{paramsFile}")) { paramMappings.singleFile.absolutePath }
                        .replace(Regex("\\{input}")) { inputJar.path.absolutePath }
                        .replace(Regex("\\{output}")) { outputJar.path.absolutePath }
                        .replace(Regex("\\{inputClasspath}")) { minecraftLibraries.files.joinToString(":") { it.absolutePath } }
                        .replace(Regex("\\{reportsDir}")) { project.buildDirectory.resolve("reports").absolutePath })
                }
            }
        }
    }
}
