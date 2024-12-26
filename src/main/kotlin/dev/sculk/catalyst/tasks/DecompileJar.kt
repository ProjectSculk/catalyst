package dev.sculk.catalyst.tasks

import dev.sculk.catalyst.util.absolutePath
import dev.sculk.catalyst.util.cleanFile
import dev.sculk.catalyst.util.path
import dev.sculk.catalyst.util.useOutput
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject
import kotlin.io.path.name
import kotlin.io.path.writeText

@CacheableTask
abstract class DecompileJar : BaseTask() {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val inputJar: RegularFileProperty
    
    @get:Input
    abstract val decompilerArgs: ListProperty<String>
    
    @get:OutputFile
    abstract val outputJar: RegularFileProperty
    
    @get:CompileClasspath
    abstract val minecraftLibraries: ConfigurableFileCollection
    
    @get:Classpath
    abstract val decompiler: ConfigurableFileCollection
    
    @get:Inject
    abstract val exec: ExecOperations

    override fun run() {
        val out = outputJar.path.cleanFile()
        val cfg = out.resolveSibling("${out.name}.cfg").cleanFile()
        
        cfg.writeText(buildString { 
            minecraftLibraries.forEach { lib -> 
                append("--add-external=")
                append(lib.absolutePath)
                append(System.lineSeparator())
            }
        })
        
        val logFile = out.resolveSibling("${out.name}.log")
        logFile.useOutput { log ->
            exec.javaexec {
                standardOutput = log
                errorOutput = log
                
                maxHeapSize = "4G"
                
                mainClass.set("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler")
                classpath(decompiler)
                
                args(decompilerArgs.get())
                args("-cfg", cfg.absolutePath)
                args(inputJar.path.absolutePath)
                args(outputJar.path.absolutePath)
            }
        }
    }
}
