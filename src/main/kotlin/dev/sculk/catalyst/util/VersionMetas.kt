package dev.sculk.catalyst.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VersionBundleInfo(
    val downloads: Downloads,
    val javaVersion: JavaVersion
) {
    @Serializable
    data class Downloads(
        val server: DownloadEntry,
        @SerialName("server_mappings")
        val mappings: DownloadEntry,
    ) {
        @Serializable
        data class DownloadEntry(
            val sha1: String,
            val size: Long,
            val url: String
        )
    }

    @Serializable
    data class JavaVersion(
        val component: String,
        val majorVersion: Int
    )
}

@Serializable
data class VersionManifest(
    val latest: LatestInfo,
    val versions: List<Entry>
) {
    @Serializable
    data class Entry(
        val id: String,
        val url: String,
        val time: String,
        val releaseTime: String,
        val sha1: String,
        val complianceLevel: Int
    )

    @Serializable
    data class LatestInfo(
        val release: String,
        val snapshot: String
    )
}
