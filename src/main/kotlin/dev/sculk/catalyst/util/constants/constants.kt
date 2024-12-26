package dev.sculk.catalyst.util.constants

val AT_PATTERN = Regex("""// AT:\s*(.+)$""")

const val INTERNAL_JAVA_SOURCE_SET = "src/minecraft/java"
const val INTERNAL_RESOURCES_SOURCE_SET = "src/minecraft/resources"

// Minecraft constants
const val MC_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

private const val CACHE_DIR = "caches/catalyst"
private const val MACHE_DIR = "caches/mache"

const val MACHE_METADATA = "$MACHE_DIR/mache.json"

const val DEOBFUSCATION_MAPPINGS = "$CACHE_DIR/vanilla/mappings.txt"
const val LIBRARIES_LIST = "$CACHE_DIR/vanilla/libraries.list"
const val VERSION_MANIFEST = "$CACHE_DIR/vanilla/version_manifest.json"
const val VERSION_DATA = "$CACHE_DIR/vanilla/version_data.json"
const val VANILLA_JAR = "$CACHE_DIR/vanilla/server.jar"

const val REMAPPED_JAR = "$CACHE_DIR/remapped.jar"
const val DECOMPILED_JAR = "$CACHE_DIR/sources.jar"
