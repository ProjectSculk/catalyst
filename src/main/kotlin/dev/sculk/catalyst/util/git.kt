package dev.sculk.catalyst.util

import org.gradle.api.file.FileSystemLocationProperty
import java.io.OutputStream
import java.io.PrintStream
import java.io.*
import java.util.*

// Originally taken from https://github.com/AirflowMC/Airflow
// Originally licensed under the MIT License

class NullPrintStream : PrintStream(NullOutputStream()) {
    override fun write(b: Int) {
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
    }

    override fun print(b: Boolean) {
    }

    override fun print(c: Char) {
    }

    override fun print(i: Int) {
    }

    override fun print(l: Long) {
    }

    override fun print(f: Float) {
    }

    override fun print(d: Double) {
    }

    override fun print(s: CharArray) {
    }

    override fun print(s: String?) {
    }

    override fun print(obj: Any?) {
    }

    override fun println() {
    }

    override fun println(x: Boolean) {
    }

    override fun println(x: Char) {
    }

    override fun println(x: Int) {
    }

    override fun println(x: Long) {
    }

    override fun println(x: Float) {
    }

    override fun println(x: Double) {
    }

    override fun println(x: CharArray) {
    }

    override fun println(x: String?) {
    }

    override fun println(x: Any?) {
    }

    override fun printf(format: String, vararg args: Any?): PrintStream {
        return this
    }

    override fun printf(l: Locale?, format: String, vararg args: Any?): PrintStream {
        return this
    }

    override fun format(format: String, vararg args: Any?): PrintStream {
        return this
    }

    override fun format(l: Locale?, format: String, vararg args: Any?): PrintStream {
        return this
    }

    override fun append(csq: CharSequence): PrintStream {
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): PrintStream {
        return this
    }

    override fun append(c: Char): PrintStream {
        return this
    }
}

class NullOutputStream : OutputStream() {
    override fun write(b: Int) {
    }
}

class GitBuilder {
    private var _args: Array<out String> = emptyArray()
    val args: Array<out String>
        get() = _args

    private var _workingDir: File? = null
    val workingDir: File?
        get() = _workingDir

    private var _silent: Boolean = false
    val silent: Boolean
        get() = _silent

    private var _silentErr: Boolean = false
    val silentErr: Boolean
        get() = _silentErr

    fun args(vararg args: String) {
        _args = args
    }

    fun workingDir(workingDir: File) {
        _workingDir = workingDir
    }

    fun workingDir(workingDir: FileSystemLocationProperty<*>) {
        workingDir(workingDir.asFile.get())
    }

    fun silent(silent: Boolean) {
        _silent = silent
    }

    fun silentErr(silentErr: Boolean) {
        _silentErr = silentErr
    }
}

fun git(function: GitBuilder.() -> Unit) {
    val builder = GitBuilder()
    function(builder)

    val process = ProcessBuilder("git", *builder.args)
        .directory(builder.workingDir)
        .start()

    redirect(process.inputStream, if (builder.silent) NullPrintStream() else System.out)
    redirect(process.errorStream, if (builder.silentErr) NullPrintStream() else System.err)

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        throw RuntimeException("git process ended with $exitCode exit code.")
    }
}

private fun redirect(`is`: InputStream, out: PrintStream) {
    val thread = Thread {
        BufferedReader(InputStreamReader(`is`)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                out.println(line)
            }
        }
    }
    thread.isDaemon = true
    thread.start()
}
