package dev.sculk.catalyst.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MacheMeta(
    val minecraftVersion: String,
    val repositories: List<Repository>,
    @SerialName("dependencies")
    val tools: Map<String, List<Dependency>>,
    @SerialName("additionalCompileDependencies")
    val sourceDependencies: Map<String, List<Dependency>>,
    val decompilerArgs: List<String>,
    val remapperArgs: List<String>
) {
    @Serializable
    data class Repository(
        val url: String,
        val name: String,
        val groups: List<String>
    )

    @Serializable
    data class Dependency(
        val group: String,
        val name: String,
        val version: String,
        val classifier: String? = null,
        val extension: String? = null
    ) {
        override fun toString(): String {
            return buildString {
                append("$group:$name:$version")
                if (classifier != null) {
                    append(":$classifier")
                }
                if (extension != null) {
                    append("@$extension")
                }
            }
        }
    }
}
