package dev.sculk.catalyst.util

import dev.sculk.catalyst.util.constants.INTERNAL_JAVA_SOURCE_SET
import dev.sculk.catalyst.util.constants.INTERNAL_RESOURCES_SOURCE_SET
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.JvmLibrary
import org.gradle.kotlin.dsl.getArtifacts
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withArtifacts
import org.gradle.language.base.artifact.SourcesArtifact
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
@Suppress("UnstableApiUsage")
object McDev {
    fun importLibrarySources(
        project: Project,
        importsFile: Path,
        patches: Iterable<Path>,
        dir: Path,
    ) {
        val runtimeClasspath = project.configurations.getByName("runtimeClasspath")
        val results = project.dependencies.createArtifactResolutionQuery()
            .forComponents(runtimeClasspath.incoming.resolutionResult.allComponents.map { it.id })
            .withArtifacts(JvmLibrary::class, SourcesArtifact::class)
            .execute()

        val libraries: List<LibrarySource> = buildList {
            results.resolvedComponents
                .map { component -> component.getArtifacts(SourcesArtifact::class) }
                .forEach { component ->
                    component
                        .map { it as ResolvedArtifactResult }
                        .forEach { result ->
                            add(
                                LibrarySource(
                                    name = result.id.displayName,
                                    path = result.file.toPath()
                                )
                            )
                        }
                }
        }

        val imports: MutableList<DevImport> = importsFile.readLines()
            .filterNot { it.isBlank() || it.startsWith("#") }
            .map { it.split(" ", limit = 2) }
            .map { (artifact, file) -> DevImport(artifact, file) }
            .toMutableList()

        imports.forEach { import ->
            val sources = libraries
                .firstOrNull { it.name.contains(import.artifact) }
                ?.path
                ?: throw IllegalArgumentException("Couldn't find '${import.artifact}' in libraries")
            val file = if (import.file.endsWith(".java")) {
                import.file
            } else {
                import.file.replace(".", "/") + ".java"
            }
            sources.openZip { jar ->
                val sourceFile = jar.resolve(file)
                if (sourceFile.notExists()) {
                    throw IllegalArgumentException("Can't find source file $file in ${import.artifact}")
                }
                val targetFile = dir.resolve(file)
                targetFile.parent.createDirectories()
                sourceFile.copyToRecursively(targetFile, overwrite = true, followLinks = false)
            }
        }

        patches.forEach { patch ->
            val patchLines = patch.readLines()
            patchLines
                .filter { it.startsWith("--- a/") }
                .map { line -> line.removePrefix("--- a/").trim() }
                .filter { it.endsWith(".java") }
                .forEach { filePath ->
                    val sourceJarContaining = libraries
                        .firstOrNull { library -> library.path.openZip { jar -> jar.resolve(filePath).exists() } }
                        ?.path
                    if (sourceJarContaining != null) {
                        val outFile = dir.resolve(filePath)
                        outFile.parent.createDirectories()
                        sourceJarContaining.openZip { jar ->
                            jar.resolve(filePath).copyToRecursively(outFile, overwrite = true, followLinks = false)
                        }
                    }
                }
        }
    }

    fun setupSourceSets(project: Project) {
        project.extensions.getByType<JavaPluginExtension>().sourceSets.named("main") {
            java {
                srcDirs(project.layout.projectDirectory.dir(INTERNAL_JAVA_SOURCE_SET))
            }
            resources {
                srcDirs(project.layout.projectDirectory.dir(INTERNAL_RESOURCES_SOURCE_SET))
            }
        }
    }
    
    fun setupClasspath(project: Project, librariesList: Path) {
        librariesList.readLines().forEach { lib ->
            val parts = lib.split('\t')
            check(parts.size == 3) { "Invalid libraries.list" }
            
            val artifact = parts[1]
            project.dependencies.add("minecraft", artifact)
        }
    }
    
    internal data class LibrarySource(val name: String, val path: Path)

    internal data class DevImport(val artifact: String, val file: String)
}
