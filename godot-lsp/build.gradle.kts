plugins {
    alias(libs.plugins.gradleIntelliJPlatform)
    alias(libs.plugins.gradleJvmWrapper)
    alias(libs.plugins.kotlinJvm)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val repoRoot = projectDir.parentFile!!

dependencies {
    intellijPlatform {
        rustRover(libs.versions.ideaSdk) { useInstaller = false }
        jetbrainsRuntime()
        localPlugin(repoRoot.resolve("community/build/distributions/rider-godot-community.zip"))
    }
}

intellijPlatform {
    instrumentCode = false
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253.31033"
        }
    }
}
