package dev.sculk.catalyst.tasks

import dev.sculk.catalyst.util.*
import dev.sculk.catalyst.util.constants.MC_MANIFEST_URL
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively

@CacheableTask
abstract class PrepareServerJar : BaseTask() {
    @get:Input
    abstract val minecraftVersion: Property<String>
    
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val mache: ConfigurableFileCollection
    
    @get:OutputFile
    abstract val versionManifest: RegularFileProperty
    
    @get:OutputFile
    abstract val versionBundleInfo: RegularFileProperty
    
    @get:OutputFile
    abstract val deobfuscationMappings: RegularFileProperty
    
    @get:OutputFile
    abstract val serverJar: RegularFileProperty
    
    @get:OutputFile
    abstract val librariesList: RegularFileProperty
    
    @get:OutputFile
    abstract val macheMetadata : RegularFileProperty
    
    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        val mcVersion = minecraftVersion.get()
        
        val manifest: VersionManifest = versionManifest
            .download(MC_MANIFEST_URL)
            .file
            .parseJson()
        
        val bundleInfo: VersionBundleInfo = versionBundleInfo
            .download(manifest.versions.find { v -> v.id == mcVersion}!!.url)
            .file
            .parseJson()
        
        deobfuscationMappings.download(bundleInfo.downloads.mappings.url)
        
        val tempBundleJar = temporaryDir.resolve("bundler.jar").toPath()
        tempBundleJar.download(bundleInfo.downloads.server.url)
        
        tempBundleJar.openZip { jar ->
            val metaInf = jar.resolve("META-INF/")
            metaInf.resolve("libraries.list")
                .copyTo(librariesList.path, overwrite = true)
            metaInf.resolve("versions/$mcVersion/server-$mcVersion.jar")
                .copyTo(serverJar.path, overwrite = true)
        }
        
        mache.singleFile.toPath().openZip { macheZip ->
            macheZip.resolve("mache.json").copyToRecursively(macheMetadata.path, overwrite = true, followLinks = false)
        }
    }
}
