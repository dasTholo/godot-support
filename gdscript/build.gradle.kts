import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import kotlin.io.path.Path
import kotlin.io.path.pathString

plugins {
    alias(libs.plugins.changelog)
    alias(libs.plugins.gradleIntelliJPlatform)
    alias(libs.plugins.gradleJvmWrapper)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.grammarkit)
    id("java")
}

allprojects {
    repositories {
        mavenCentral()
    }
}

val repoRoot = projectDir.parentFile!!
sourceSets.getByName("main") {
    java {
        srcDir(repoRoot.resolve("gdscript/src/main/gen"))
    }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

val buildConfiguration: String by project

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.ideaSdk) { useInstaller = false }
        // rider(libs.versions.riderSdk, useInstaller = false) // instead of touching this, just use runRider gradle task
        jetbrainsRuntime()
        testFramework(TestFrameworkType.Bundled)

        bundledPlugin("com.intellij.modules.json")
        bundledModule("intellij.platform.dap")

        bundledLibrary(provider {
            project.intellijPlatform.platformPath.resolve("lib/testFramework.jar").pathString
        })
    }
    implementation(libs.jflex)
    testImplementation(libs.openTest4J)
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
}

intellijPlatform{
    instrumentCode = false
    buildSearchableOptions = buildConfiguration != "Debug"
    pluginConfiguration {
        version = "1.0.0"
        ideaVersion {
            sinceBuild = "253.31033"
        }
    }
}

grammarKit {
    // todo: figure out later
}

val lexers = listOf(
    Triple("config", "config", "src/main/kotlin/config/GdConfig.flex"),
    Triple("gdscript", "gdscript", "src/main/kotlin/gdscript/Gd.flex"),
    Triple("gdscriptHighlighter", "gdscript", "src/main/kotlin/gdscript/GdHighlight.flex"),
    Triple("tscn", "tscn", "src/main/kotlin/tscn/Tscn.flex"),
    Triple("project", "project", "src/main/kotlin/project/Project.flex"),
)

lexers.forEach { (lexerName, folder, lexerPath) ->
    project.tasks.register<GenerateLexerTask>("${lexerName}Lexer") {
        sourceFile = file(lexerPath)
        targetOutputDir = file("src/main/gen/$folder")
        purgeOldFiles.set(false)
    }
}

tasks {
    compileKotlin {
        dependsOn( lexers.map { "${it.first}Lexer" })
    }
    
    register("prepare") {
        doLast {
            val cacheDir = File(System.getProperty("user.home"), ".cache/godot-gdscript-sdk")
            val sdkFile = cacheDir.resolve("sdk.tar.xz")
            if (sdkFile.exists()) {
                logger.lifecycle("SDK cache hit: ${sdkFile.absolutePath}")
                return@doLast
            }
            logger.lifecycle("SDK cache miss, building SDK...")
            sdk.SdkBuilder.build(cacheDir)
        }
    }

    prepareSandbox{
        dependsOn("prepare")
        val pluginName = intellijPlatform.projectName.get()
        val cacheDir = File(System.getProperty("user.home"), ".cache/godot-gdscript-sdk")
        from(cacheDir) { into(Path(pluginName, "sdk").pathString)}
    }

    // run it to start Rider from SDK
    val runRustRover by intellijPlatformTesting.runIde.registering {
        type = IntelliJPlatformType.RustRover
        version = libs.versions.riderSdk
        useInstaller = false
        task {
            enabled = true
            dependsOn(prepareSandbox)

            val pluginName = intellijPlatform.projectName.get()
            val cacheDir = File(System.getProperty("user.home"), ".cache/godot-gdscript-sdk")

            // sandboxPluginsDirectory is not adequate when calling runRider
            val target2 = Path(sandboxDirectory.get().asFile.absolutePath, "plugins_runRustRover", pluginName, "sdk")
            logger.lifecycle("Copying SDK from $cacheDir to $target2")
            project.copy {
                from(cacheDir)
                into(target2)
            }
        }
    }

    runIde {
        jvmArgs("-Xmx1500m")
    }

    test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
        environment["LOCAL_ENV_RUN"] = "true"
    }
}