# SDK Builder: PHP zu Kotlin Rewrite

**Ziel:** Die 5 PHP-Dateien des SDK-Builders durch Kotlin-Code in `buildSrc/` ersetzen, sodass `./gradlew prepare` das SDK ohne externe Dependencies (kein PHP) bauen kann.

**Motivation:** PHP ist nicht installiert und soll als externe Dependency entfallen. Die JVM-Toolchain (JDK 21) ist bereits vorhanden.

## Architektur

### Dateistruktur

```
gdscript/buildSrc/
  build.gradle.kts                    # kotlin-jvm, commons-compress
  src/main/kotlin/sdk/
    SdkBuilder.kt                      # Orchestrator
    ClassParser.kt                     # XML -> .gd Stubs
    OperandParser.kt                   # XML -> operators.gdconf
    AnnotationParser.kt                # XML -> annotation.gdconf
    TemplateParser.kt                  # Godot Templates -> IntelliJ .ft
```

### Komponentenbeschreibung

**SdkBuilder** (ersetzt `sdkBuilder.php` + `build-sdk.sh`)
- Fetcht Tags via `git ls-remote --tags https://github.com/godotengine/godot`
- Filtert auf >= 4.6 (Regex + Minor-Version-Check)
- Laedt Tarballs von GitHub herunter (`java.net.http.HttpClient`)
- Entpackt `.tar.gz` in temporaeres Verzeichnis
- Ruft die drei Parser auf pro Version
- Baut zusaetzlich "Master" als spezielle Version
- Packt Ergebnis als `sdk.tar.xz` (`commons-compress`)

**ClassParser** (ersetzt `classParser.php`, ~296 Zeilen PHP)
- Input: Godot XML-Docs aus `doc/classes/` und Module `doc_classes/`
- Output: Ein `.gd` Stub-File pro Klasse in `{version}/`
- Parsing: `javax.xml.parsers.DocumentBuilderFactory`
- Generiert: Klassen-Header, Signals, Constants/Enums, Member-Variablen, Methoden mit `pass;`
- Konvertiert Array-Syntax (`Type[]` -> `Array[Type]`)
- Formatiert Docs als `##` Kommentare

**OperandParser** (ersetzt `operandParser.php`, ~75 Zeilen PHP)
- Input: Gleiche XML-Docs
- Output: `operators.gdconf`
- Format: `OP {LeftType}\n{operator} {RightType} : {ResultType}`
- Filtert unaere Operatoren und `~`

**AnnotationParser** (ersetzt `annotationParser.php`, ~44 Zeilen PHP)
- Input: `modules/gdscript/doc_classes/@GDScript.xml`
- Output: `annotation.gdconf`
- Format: `AN {variadic} {required_count} {name} {params}`

**TemplateParser** (ersetzt `templateParser.php`, ~31 Zeilen PHP)
- Input: `modules/gdscript/editor/script_templates/`
- Output: IntelliJ `.ft` Dateien in `src/main/resources/fileTemplates/`
- Ersetzt `_BASE_` durch `${NAME}`

### Gradle-Integration

Der bestehende `register("prepare")` Task in `build.gradle.kts` wird umgeschrieben:

```kotlin
register("prepare") {
    doLast {
        val sdkDir = project.layout.buildDirectory.dir("sdk").get().asFile
        val sdkFile = sdkDir.resolve("sdk.tar.xz")
        if (sdkFile.exists()) return@doLast

        // Lokal bauen
        SdkBuilder.build(sdkDir)
    }
}
```

Kein Fallback auf Remote-Download mehr noetig — das SDK wird immer lokal gebaut.

### Dependencies (nur in buildSrc)

| Dependency | Zweck | Quelle |
|-----------|-------|--------|
| `javax.xml` | XML-Parsing | JDK built-in |
| `java.net.http` | HTTP Downloads | JDK built-in |
| `org.apache.commons:commons-compress` | tar.xz lesen/schreiben | Maven Central |

### Was entfaellt

- `gdscript/php/sdkBuilder.php`
- `gdscript/php/classParser.php`
- `gdscript/php/operandParser.php`
- `gdscript/php/annotationParser.php`
- `gdscript/php/templateParser.php`
- `gdscript/build-sdk.sh`
- PHP als System-Dependency

### Verifizierung

1. `./gradlew prepare` baut SDK ohne PHP
2. Archiv enthaelt nur 4.6+ Versionen und höher + Master
3. Generierte `.gd` Stubs und `.gdconf` Dateien sind identisch mit PHP-Output
4. `./gradlew buildPlugin` erfolgreich mit dem neuen SDK
