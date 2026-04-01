# Local setup

This repository contains the GdScript plugin developed with Kotlin for JetBrains IDEs. Below are the up-to-date steps to set up a local development environment.

Requirements in your IDE:
- IntelliJ IDEA Community or Ultimate (recommended) installed.
- Plugin DevKit plugin installed (Settings/Preferences -> Plugins -> Marketplace -> "Plugin DevKit").
- Gradle is managed by IntelliJ automatically; verify under Settings/Preferences -> Build, Execution, Deployment -> Build Tools -> Gradle.
- Optional: JFlex plugin (for regenerating lexers from .flex files).
- Optional: GrammarKit (parsers for .gd are no longer used; .tscn still uses GrammarKit-generated code).

Alternatively, follow the official prerequisites: https://plugins.jetbrains.com/docs/intellij/prerequisites.html

Running the plugin in IDE (Gradle runIde):
- `./gradlew buildPlugin` from the Terminal in the gdscript folder.
- Then run the `runRustRover` task from the Gradle tool window.

Target IDE version/type:
- The project uses the IntelliJ Platform Gradle Plugin (intellijPlatform {} DSL) and pins the IDE version via gradle/libs.versions.toml (libs.versions.ideaSdk).
- To experiment with a different IDE version or type, adjust the intellijPlatform dependencies in build.gradle.kts.

Troubleshooting:
- After changing Gradle files, always Reload All Gradle Projects.
