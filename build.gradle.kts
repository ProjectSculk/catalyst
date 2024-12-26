plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    id("com.gradle.plugin-publish") version "1.3.0"
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

java {
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // for reading json metadata
    implementation("org.cadixdev:at:0.1.0-rc1") // at generation
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")
    implementation("codechicken:DiffPatch:1.5.0.30") // for applying and rebuilding patches
}

// <editor-fold desc="Publishing">
val isSnapshot = version.toString().endsWith("SNAPSHOT")

publishing {
    repositories {
        maven {
            name = "sculk"
            url = uri("https://maven.sculk.dev/" + if (isSnapshot) "snapshots" else "releases")
            credentials(PasswordCredentials::class)
        }
    }
}

gradlePlugin {
    website = "https://github.com/ProjectSculk/catalyst"
    vcsUrl = "https://github.com/ProjectSculk/catalyst.git"
    plugins {
        create("catalystPlugin") {
            id = "dev.sculk.catalyst"
            displayName = "Catalyst"
            description = "A gradle plugin for creating minecraft server forks"
            implementationClass = "dev.sculk.catalyst.CatalystPlugin"
        }
    }
}
// </editor-fold>
