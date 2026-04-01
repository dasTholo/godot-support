# CLAUDE.md

## Language

- **Chat:** German | **Code, commits, PRs:** English

## Project

Community fork of [JetBrains/godot-support](https://github.com/JetBrains/godot-support) for **RustRover**. Branch `253-unified` merges all plugins into one **gdscript** module. Vendor: DasTholo.

## Build & Test

All commands from `gdscript/`. Prerequisites: JDK 21.

```bash
cd gdscript
./gradlew build                        # Compile + tests
./gradlew buildPlugin                  # Plugin ZIP
./gradlew compileKotlin                # Compile only
./gradlew runRustRover                 # Sandboxed RustRover
```

**Always use `--no-daemon` for tests** — otherwise the Gradle daemon stays alive and background runs appear to hang:

```bash
./gradlew test --console=plain --no-daemon              # All tests
./gradlew test --console=plain --no-daemon --tests "..." # Single test
```

Hanging test? `./gradlew --status` → `jstack <PID>` → `./gradlew --stop`

## Architecture

Kotlin, Java 21 toolchain, IntelliJ Platform Gradle Plugin.

**Source** (`gdscript/src/main/kotlin/`): `gdscript/` (parser, lexer, PSI, completion, formatter, references, inspections), `tscn/` (scene files), `project/` (project.godot), `config/`, `common/`

**Parser:** Handwritten Kotlin (context-sensitive, indent-based). Not BNF-generated — old `gd.bnf`/`GdParser.java` are obsolete. Two lexers: `Gd.flex` (grammar) + `GdHighlight.flex` (highlighter). Generated sources in `src/main/gen/`.

**Integrations:** LSP (Godot editor, port 6005), DAP (debugging), GDExtension/Rust navigation (`GdExtensionRustResolver`, regex-based).

**SDK Stubs:** Built locally from Godot sources via `gdscript/buildSrc/src/main/kotlin/sdk/SdkBuilder`. Fetches stable tags >= 4.6 from godotengine/godot.

**Lexer generation:** Automatic on `compileKotlin` when `.flex` files change.

## Testing

JUnit 4 on JUnit Platform (Vintage). `BasePlatformTestCase` for IDE fixtures, `ParsingTestCase` for PSI tests. Golden files in `src/test/testData/gdscript/`. Running all tests at once is faster than individual.

## Target Platform

RustRover 2025.3 (Build 253.31033+) · Godot 4.6+ · IntelliJ IDEA Community 2025.3 SDK

## Design Specs

`docs/superpowers/specs/` (German)
