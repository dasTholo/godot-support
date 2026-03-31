# Plugin-Zusammenfuehrung: Community + Godot-LSP + GdScript → Ein Plugin

## Ziel

Die drei separaten Plugins (community, godot-lsp, gdscript) zu einem einzigen Plugin zusammenfuehren. Ein Install statt drei, kein ZIP-Extraktionsproblem, einfacheres Build-Setup.

## Ausgangslage

### Aktuelle Struktur (Branch 253)

Drei separate Gradle-Module mit eigenen `plugin.xml`:

| Modul | Plugin ID | Inhalt |
|-------|-----------|--------|
| `community/` | `com.intellij.rider.godot.community` | GdLanguage, GdFileType, LspRunningStatusProvider, ProjectInfoProvider, GodotCommunityBundle |
| `godot-lsp/` | `godot.lsp` | LSP-Client, ServerSupportProvider, Settings, Widget |
| `gdscript/` | `com.intellij.rider.godot.gdscript` | Parser, PSI, Inspections, Completions, SDK, DAP, Formatter, etc. |

Dependency-Kette: `gdscript` → `community` ← `godot-lsp`

### Branch 253-merge (aktueller upstream)

Auf `253-merge` hat JetBrains upstream bereits:
- **LSP nach gdscript verschoben** (`RIDER-133041`) — LSP-Code lebt unter `gdscript/lsp/`
- **Start Godot Editor nach community verschoben** (`RIDER-135694`) — community hat Actions, Metadata-Services, FileWatcher, Utils
- **rider→rustrover Rename** durchgefuehrt — Plugin IDs, Packages, Icons

Community auf `253-merge` ist deutlich groesser (18 Kotlin-Dateien + Icons vs. 5 auf `253`).

### Branch 253 (unsere Aenderungen)

32 Commits die auf `253-merge` fehlen:
- **SDK-Builder:** Kompletter PHP→Kotlin Rewrite in `buildSrc/` (8 Commits)
- **GdExtension-Fixes:** Signal-Parameter, nested Generics, inherited Members Filter (5 Commits)
- **Parser-Fixes:** Multiline Lambda DEDENT+COMMA (2 Commits)
- **False-Positive TDD:** Test-Base, red/green Tests, typed stubs, varargs (4 Commits)
- **Test-Infrastruktur:** Array-Tests, chained method resolution, get_main_loop (6 Commits)
- **Gradle/Version:** Gradle 9, IntelliJ Platform Plugin 2.13.0, Godot 4.6+ Check (3 Commits)
- **Docs:** Specs, Plans (4 Commits)

## Architektur

### Ziel-Package-Struktur

Alles in `gdscript/src/main/kotlin/`:

```
gdscript/                          # bestehend — Parser, PSI, Inspections, Completions
gdscript/lsp/                      # godot-lsp Dateien (existiert schon auf 253-merge)
project/                           # aus community — Projekterkennung, Metadata, Services
project/actions/                   # aus community — Start Godot Editor, Toolbar
project/run/                       # aus community — GodotRunContextUtil
project/utils/                     # aus community — GodotCommunityUtil, GodotFileUtil
config/                            # bestehend
tscn/                              # bestehend + TscnFileType/TscnLanguage aus community
```

### Datei-Mapping

| Community-Datei (253-merge) | Ziel in gdscript |
|---|---|
| `GdLanguage.kt`, `GdFileType.kt` | `gdscript/` |
| `GodotCommunityBundle.kt` | In bestehenden `GdScriptBundle` mergen |
| `GodotMetadataService.kt`, `GodotProjectProvider.kt` | `project/` |
| `GodotMetadataFileWatcher.kt`, `GodotMetadataFileWatcherManager.kt` | `project/` |
| `GodotEditorConnectionProvider.kt`, `GodotMajorVersion.kt` | `project/` |
| `GodotActionsToolbar.kt`, `StartGodotEditorAction.kt`, `GodotEditorLaunchConfig.kt` | `project/actions/` |
| `GodotCommunityUtil.kt`, `GodotFileUtil.kt` | `project/utils/` |
| `GodotRunContextUtil.kt` | `project/run/` |
| `TscnFileType.kt`, `TscnLanguage.kt` | `tscn/` |
| Icons (GDScript.svg, Godot*.svg, TscnFile.svg) | `resources/icons/` |
| `GodotCommunityBundle.properties` | In bestehende Properties mergen |

| Godot-LSP-Datei | Ziel in gdscript |
|---|---|
| Bereits auf `253-merge` nach `gdscript/lsp/` verschoben | Pruefen ob vollstaendig |
| `GodotLspBundle.kt`, `GodotLspIcons.kt`, `GodotUtil.kt` | `gdscript/lsp/` |
| `service/GodotLsp4jClient.kt`, etc. | `gdscript/lsp/service/` |
| `settings/GodotLspSettings.kt`, etc. | `gdscript/lsp/settings/` |
| Icons, Messages | `resources/` |

### Plugin-XML Vereinigung

Eine einzige `plugin.xml` mit:
- Plugin ID: `com.intellij.rustrover.godot.gdscript`
- Alle Extensions aus community + godot-lsp + gdscript
- Extension Points (`lspStatusProvider`, `projectInfoProvider`) werden inline — da alles im selben Plugin, koennen die Interfaces direkt genutzt werden statt ueber Extension Points
- Kein `<depends>` auf community oder godot-lsp mehr
- `<depends>com.intellij.modules.platform</depends>` bleibt

### Build-Vereinfachung

- `settings.gradle.kts`: `includeBuild("../community")` und `includeBuild("../godot-lsp")` entfernen
- `build.gradle.kts`: `compileOnly(":rider-godot-community")` und `compileOnly(":godot-lsp")` entfernen
- Kein separater Community-Build mehr noetig
- Ein `./gradlew buildPlugin` baut alles

## Phasen

### Phase 1: Branch vorbereiten
1. Neuen Branch `253-unified` von `253-merge` erstellen
2. `253` in `253-unified` mergen
3. Konflikte loesen (hauptsaechlich rider→rustrover Renames in unseren Dateien)
4. Verifizieren dass alle 32 Commits aus `253` enthalten sind
5. Build + Tests grueen

### Phase 2: Community → gdscript verschieben
1. Dateien gemaess Mapping verschieben
2. Packages umbenennen (von `com.jetbrains.rustrover.godot.community.*` nach Ziel-Packages)
3. Alle Imports in gdscript aktualisieren
4. `GodotCommunityBundle` in `GdScriptBundle` mergen
5. Icons/Resources verschieben
6. Build kompiliert

### Phase 3: Godot-LSP → gdscript (falls noetig)
1. Pruefen was auf `253-merge` bereits verschoben wurde (`RIDER-133041`)
2. Restliche Dateien verschieben
3. `GodotLspBundle` mergen oder als eigenes Bundle belassen
4. Build kompiliert

### Phase 4: plugin.xml vereinen
1. Extensions aus community `plugin.xml` uebernehmen
2. Extensions aus godot-lsp `plugin.xml` uebernehmen
3. Extension Points inlinen oder beibehalten
4. `<depends>` auf community/godot-lsp entfernen
5. Plugin ID pruefen

### Phase 5: Module loeschen und aufraeumen
1. `community/` Modul komplett loeschen
2. `godot-lsp/` Modul komplett loeschen
3. `settings.gradle.kts` bereinigen
4. `build.gradle.kts` Dependencies bereinigen
5. Build + Tests verifizieren
6. Plugin-ZIP pruefen (enthaelt alle Klassen, Icons, Resources)

## Risiken

- **Merge-Konflikte Phase 1:** 32 Commits muessen mit dem upstream-Stand kompatibel gemacht werden. Die rider→rustrover Renames betreffen dieselben Dateien die wir geaendert haben.
- **Import-Lawine Phase 2:** ~34 Dateien importieren `com.jetbrains.rustrover.godot.community.*` — mechanisch aber fehleranfaellig.
- **Extension Points:** Falls andere Plugins die Extension Points `lspStatusProvider` oder `projectInfoProvider` nutzen, duerfen diese nicht einfach entfernt werden. Auf unserem Fork unwahrscheinlich.

## Nicht im Scope

- Dynamische SDK-Generierung via `godot --doctool` (separates Projekt, siehe Plugin-README.md)
- GDExtension Rust PSI Migration (niedrige Prioritaet)
- Rider-spezifische Module aufraeumen (separates Projekt, muss genauer geprueft werden)
