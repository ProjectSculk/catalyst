package dev.sculk.catalyst

import dev.sculk.catalyst.tasks.*
import dev.sculk.catalyst.util.*
import dev.sculk.catalyst.util.constants.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import java.util.function.Predicate
import kotlin.io.path.extension

class CatalystPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val catalyst = target.extensions.create("catalyst", CatalystExt::class)
        
        val mache by target.configurations.registering {
            isTransitive = false
        }
        
        val jst by target.configurations.registering {
            isTransitive = false
        }
        
        val codebook by target.configurations.registering {
            isTransitive = false
        }
        
        val paramMappings by target.configurations.registering {
            isTransitive = false
        }
        
        val remapper by target.configurations.registering {
            isTransitive = false
        }
        
        val decompiler by target.configurations.registering {
            isTransitive = false
        }
        
        target.afterEvaluate {
            val serverProject = catalyst.serverProject.get()
            
            // <editor-fold desc="Configurations">
            val minecraft by serverProject.configurations.registering
            serverProject.configurations.named("implementation") {
                extendsFrom(minecraft.get())
            }
            // </editor-fold>
            // <editor-fold desc="Tasks">
            val prepareServerJar by target.tasks.registering(PrepareServerJar::class) {
                minecraftVersion.set(target.providers.gradleProperty("minecraftVersion"))
                this.mache.from(mache)
                versionManifest.set(target.gradleFile(VERSION_MANIFEST))
                versionBundleInfo.set(target.gradleFile(VERSION_DATA))
                deobfuscationMappings.set(target.gradleFile(DEOBFUSCATION_MAPPINGS))
                serverJar.set(target.gradleFile(VANILLA_JAR))
                librariesList.set(target.gradleFile(LIBRARIES_LIST))
                macheMetadata.set(target.gradleFile(MACHE_METADATA))
            }

            val remapJar by target.tasks.registering(RemapJar::class) {
                dependsOn(prepareServerJar)

                val macheMetadata = prepareServerJar.get().macheMetadata.get()
                    .asFile
                    .parseJson<MacheMeta>()

                inputJar.set(prepareServerJar.flatMap { it.serverJar })
                mappings.set(prepareServerJar.flatMap { it.deobfuscationMappings })
                this.paramMappings.from(paramMappings)
                remapperArgs.set(macheMetadata.remapperArgs)
                outputJar.set(target.gradleFile(REMAPPED_JAR))
                minecraftLibraries.from(minecraft)
                this.codebook.from(codebook)
                this.remapper.from(remapper)
            }

            val decompileJar by target.tasks.registering(DecompileJar::class) {
                dependsOn(remapJar)

                val macheMetadata = prepareServerJar.get().macheMetadata.get()
                    .asFile
                    .parseJson<MacheMeta>()

                inputJar.set(remapJar.flatMap { it.outputJar })
                decompilerArgs.set(macheMetadata.decompilerArgs)
                outputJar.set(target.gradleFile(DECOMPILED_JAR))
                minecraftLibraries.from(minecraft)
                this.decompiler.from(decompiler)
            }

            val setupJavaSources by target.tasks.registering(SetupSources::class) {
                dependsOn(decompileJar)
                
                inputFile.set(decompileJar.flatMap { it.outputJar })
                devImports.set(catalyst.devImports)
                patchesDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir("patches/files") })
                this.mache.from(mache)
                atFile.set(catalyst.accessTransformers)
                outputDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir(INTERNAL_JAVA_SOURCE_SET) })
                this.serverProject.set(catalyst.serverProject)
                base.set(target.gradleFile("caches/sources"))
                this.jst.from(jst)
                minecraftLibraries.from(minecraft)
                filter.set(Predicate { it.extension == "java" })
            }

            val setupResources by target.tasks.registering(SetupSources::class) {
                dependsOn(remapJar)
                
                inputFile.set(remapJar.flatMap { it.outputJar })
                patchesDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir("patches/resources") })
                outputDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir(INTERNAL_RESOURCES_SOURCE_SET) })
                base.set(target.gradleFile("caches/resources"))
                filter.set(Predicate { it.extension != "java" && it.extension != "class" && !it.toString().contains("META-INF/") })
            }

            val applySourceFilePatches by target.tasks.registering(ApplyFilePatches::class) {
                dependsOn(setupJavaSources)
                group = "catalyst"
                
                outputDir.set(setupJavaSources.flatMap { it.outputDir })
                patches.set(setupJavaSources.flatMap { it.patchesDir })
                rejects.set(target.layout.buildDirectory.dir("rejects/sources"))
            }
            
            val applyFeaturePatches by target.tasks.registering(ApplyFeaturePatches::class) {
                dependsOn(applySourceFilePatches)
                group = "catalyst"
                
                output.set(setupJavaSources.flatMap { it.outputDir })
                patchDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir("patches/features") })
            }

            val applyResourcePatches by target.tasks.registering(ApplyFilePatches::class) {
                dependsOn(setupResources)
                group = "catalyst"
                
                outputDir.set(setupResources.flatMap { it.outputDir })
                patches.set(setupResources.flatMap { it.patchesDir })
                rejects.set(target.layout.buildDirectory.dir("rejects/resources"))
            }

            val rebuildSourceFilePatches by target.tasks.registering(RebuildFilePatches::class) {
                group = "catalyst"
                inputDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir(INTERNAL_JAVA_SOURCE_SET) })
                patchDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir("patches/files") })
                accessTransforms.set(catalyst.accessTransformers)
                base.set(target.gradleFile("caches/sources"))
            }
            
            val rebuildFeaturePatches by target.tasks.registering(RebuildFeaturePatches::class) {
                group = "catalyst"
                inputDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir(INTERNAL_JAVA_SOURCE_SET) })
                patchDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir("patches/features") })
            }

            val rebuildResourcePatches by target.tasks.registering(RebuildFilePatches::class) {
                group = "catalyst"
                inputDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir(INTERNAL_RESOURCES_SOURCE_SET) })
                patchDir.set(catalyst.serverProject.map { it.layout.projectDirectory.dir("patches/resources") })
                base.set(target.gradleFile("caches/resources"))
            }

            val applyFilePatches = target.tasks.create("applyFilePatches") {
                group = "catalyst"
                description = "Applies all file patches to minecraft resources and sources"
                dependsOn(applyResourcePatches, applySourceFilePatches)
            }

            val rebuildFilePatches = target.tasks.create("rebuildFilePatches") {
                group = "catalyst"
                description = "Rebuilds all file patches for minecraft resources and sources"
                dependsOn(rebuildResourcePatches, rebuildSourceFilePatches)
            }
            
            target.tasks.create("applyAllPatches") {
                group = "catalyst"
                description = "Applies both file and feature patches"
                dependsOn(applyFilePatches, applyFeaturePatches)
            }
            
            target.tasks.create("rebuildAllPatches") {
                group = "catalyst"
                description = "Rebuilds all patches"
                dependsOn(rebuildFilePatches, rebuildFeaturePatches)
            }
            // </editor-fold>
            
            val prepareTask = prepareServerJar.get()
            
            prepareTask.run()

            val macheMetadata = prepareTask.macheMetadata.get()
                .asFile
                .parseJson<MacheMeta>()
            
            macheMetadata.repositories.forEach { repo ->
                repositories.maven {
                    name = repo.name
                    url = uri(repo.url)
                    if (repo.groups.isNotEmpty()) {
                        content {
                            repo.groups.forEach { group ->
                                includeGroup(group)
                            }
                        }
                    }
                }
            }

            (macheMetadata.sourceDependencies + macheMetadata.tools).forEach { (cfg, artifacts) -> 
                artifacts.forEach { artifact ->
                    dependencies.add(cfg, artifact.toString())
                }
            }
            
            McDev.setupSourceSets(serverProject)
            McDev.setupClasspath(serverProject, prepareTask.librariesList.path)
        }
    }
}
