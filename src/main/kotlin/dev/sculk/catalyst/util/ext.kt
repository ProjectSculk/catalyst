package dev.sculk.catalyst.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import java.io.BufferedOutputStream
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.stream.StreamSupport
import kotlin.io.path.*
import kotlin.streams.asSequence

val Provider<out FileSystemLocation>.file: File
    get() = get().asFile

val Provider<out FileSystemLocation>.path: Path
    get() = file.toPath()

val RegularFile.path: Path
    get() = asFile.toPath()

val Project.gradleDir: Path
    get() = rootDir.resolve(".gradle/").toPath()

fun Project.gradlePath(path: String): Path = gradleDir.resolve(path).also {
    it.parent.createDirectories()
}

fun Project.gradleFile(path: String): File = gradlePath(path).toFile()

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

fun File.download(url: URL) = writeBytes(url.readBytes()).also {
    Logging.getLogger(Logger.ROOT_LOGGER_NAME).lifecycle("Downloading $url")
}

fun <T : Any> T.download(link: Any): T  {
    val url: URL = when (link) {
        is String -> URI(link).toURL()
        is URI -> link.toURL()
        is URL -> link
        else -> throw IllegalArgumentException("Link cannot be converted to a URL")
    }
    when (this) {
        is File -> download(url)
        is Path -> this.toFile().download(url)
        is FileSystemLocation -> this.asFile.download(url)
        is Provider<*> -> this.get().download(link)
        else -> throw IllegalArgumentException("Unrecognized type representing a file")
    }
    return this
}

inline fun <T> Path.openZip(block: (Path) -> T): T {
    return FileSystems.newFileSystem(this).use { f ->
        val root = f.getPath("/")
        block(root)
    }
}

inline fun <reified T> File.parseJson() = json.decodeFromString<T>(readText())

fun Path.useOutput(block: (BufferedOutputStream) -> Unit) = outputStream().buffered().use { block(it) }

val Project.buildDirectory: Path
    get() = layout.buildDirectory.path

val Path.absolutePath: String
    get() = absolutePathString()


inline fun <reified T> Any.json(): String = json.encodeToString<T>(this as T)

fun Path.cleanFile(): Path = this.apply {
    deleteIfExists()
}

private fun Path.jarUri(): URI {
    return URI.create("jar:${toUri()}")
}

fun Path.openFs(): FileSystem {
    return try {
        FileSystems.getFileSystem(jarUri())
    } catch (e: FileSystemNotFoundException) {
        FileSystems.newFileSystem(jarUri(), emptyMap<String, Any>())
    }
}

@OptIn(ExperimentalPathApi::class)
fun FileSystem.walkSequence(vararg options: PathWalkOption): Sequence<Path> {
    return StreamSupport.stream(rootDirectories.spliterator(), false)
        .asSequence()
        .flatMap { it.walk(*options) }
}
