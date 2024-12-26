package dev.sculk.catalyst.util


import java.io.BufferedWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.io.path.*
import org.cadixdev.at.AccessChange
import org.cadixdev.at.AccessTransform
import org.cadixdev.at.AccessTransformSet
import org.cadixdev.at.ModifierChange
import org.cadixdev.at.io.AccessTransformFormat

fun AccessTransformFormat.writeLF(path: Path, at: AccessTransformSet, header: String? = null) {
    val stringWriter = StringWriter()
    val writer = BufferedWriter(stringWriter)
    write(writer, at)
    writer.close()
    val lines = header?.let { mutableListOf(it) } ?: mutableListOf()
    lines += stringWriter.toString()
        .replace("\r\n", "\n")
        .split("\n")
        .filter { it.isNotBlank() }
        .sorted()
    path.writeText(lines.joinToString(separator = "\n", postfix = "\n"))
}

fun AccessTransformFormat.fromString(input: String): AccessTransform {
    var last = input.length - 1

    val final = if (input[last] == 'f') {
        if (input[--last] == '-') ModifierChange.REMOVE else ModifierChange.ADD
    } else {
        ModifierChange.NONE
    }

    val access = when (input.split("+", "-").first()) {
        "public" -> AccessChange.PUBLIC
        "protected" -> AccessChange.PROTECTED
        "private" -> AccessChange.PRIVATE
        else -> AccessChange.NONE
    }

    return AccessTransform.of(access, final)
}
