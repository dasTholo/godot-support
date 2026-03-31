# Godot Tools for RustRover

Ziel: Die nützlichen, IDE-agnostischen Features aus dem JetBrains Godot-Support-Repository als Plugins für RustRover (und andere nicht-Rider JetBrains IDEs) verfügbar machen.

## Ausgangslage

Das offizielle [JetBrains/godot-support](https://github.com/JetBrains/godot-support) Repository enthält neben dem GDScript-Sprachplugin zahlreiche Godot-Integrations-Features im `rider/`-Modul. Diese sind an Rider (C#/.NET) gekoppelt, obwohl viele davon auf Standard-IntelliJ-Platform-APIs basieren und prinzipiell in jeder JetBrains IDE funktionieren.

Dieses Projekt extrahiert diese Features und macht sie für RustRover 2025.3 (Build 253) nutzbar.

## Plugin-Architektur

Die Portierung erfolgt in drei einzeln testbaren Plugins, die langfristig zu einem einzigen **Godot Tools** Plugin zusammengeführt werden.

### 1. Godot LSP

Verbindet RustRover mit dem GDScript Language Server des laufenden Godot-Editors.

**Features:**
- Auto-Connect zum Godot Language Server (Port 6005)
- Live-Diagnostics (Fehler/Warnungen direkt vom Godot-Editor)
- LSP-Status-Widget in der Statusleiste
- Godot-spezifische LSP-Notifications

**Quellklassen (portierbar):**
- `GodotLspServerSupportProvider` -- Standard `LspServerSupportProvider`
- `GodotLsp4jClient` -- Standard `Lsp4jClient`
- `GodotLspServerWidgetItem` -- Standard `LspServerWidgetItem`

**Anpassungsbedarf:**
- `GodotLspNotification` -- RD-Lifetime durch Standard-Disposable ersetzen
- `GodotLspProjectService` -- RD-Reactive durch Coroutines/Flow ersetzen

### 2. Godot Project

Erkennt Godot-Projekte und optimiert die IDE-Konfiguration.

**Features:**
- Automatische Erkennung von Godot-Projekten via `project.godot`
- `.godot/`-Verzeichnis vom Index ausschliessen
- Datei-Nesting-Rules in der Projektansicht (`.tscn`/`.tres` gruppieren)
- Metadata-File-Watcher fuer `project.godot` Aenderungen

**Quellklassen (portierbar):**
- `GodotNestingRulesProvider` -- Standard `ProjectViewNestingRulesProvider`
- `GodotMetadataFileWatcher` -- Standard `AsyncFileListener`
- `GodotMetadataFileWatcherUtil` -- Pure Utility-Klasse
- `Util` -- Dateipfad-Hilfsfunktionen

**Anpassungsbedarf:**
- `GodotFilesIndexingRuleProvider` -- Rider-IndexingRule durch Standard-Exclude ersetzen
- `GodotProjectDiscoverer` -- RD-Protocol komplett durch simplen `ProjectActivity` ersetzen
- `GodotMetadataFileWatcherManager` -- RD-Lifetime durch Standard-Disposable ersetzen

### 3. Godot Runner

Godot-Szenen direkt aus der IDE starten und debuggen.

**Features:**
- GDScript Run Configuration (Szene auswählen und starten)
- DAP-basiertes GDScript-Debugging (Breakpoints, Step-through)
- Rechtsklick auf `.tscn` -> "Run Scene" / "Debug Scene"
- Godot-Editor-Start-Action in der Toolbar

**Quellklassen (portierbar):**
- `GdScriptConfigurationType` -- Standard `ConfigurationTypeBase`
- `GdScriptRunConfiguration` -- Standard `RunConfigurationBase` + DAP
- `GdScriptRunFactory` -- Standard `ConfigurationFactory`
- `GdScriptDebugAdapterSupportProvider` -- Standard DAP-Framework
- `GdScriptDapLaunchArgumentsProvider` -- Standard DAP-Launch
- `GdScriptLineBreakpointType` -- Standard `XLineBreakpointTypeBase`
- `GdScriptExceptionBreakpointType` -- Standard `XBreakpointType`
- `DebugSceneRunConfigurationProducer` -- Standard `LazyRunConfigurationProducer`
- `GdScriptSceneRunConfigurationProducer` -- Standard Producer

**Anpassungsbedarf:**
- `StartGodotEditorAction` -- Rider-ExeConfigurationType durch generischen Prozess-Start ersetzen

## Zusammenführung: Godot Tools

Nach erfolgreicher Einzeltestung werden alle drei Plugins zu einem **Godot Tools** Plugin zusammengeführt:

```
godot-tools/
  src/main/
    kotlin/
      lsp/          # Godot LSP
      project/      # Godot Project
      runner/       # Godot Runner
    resources/
      META-INF/
        plugin.xml  # Vereinte Plugin-Konfiguration
```

**Vorteile eines einzelnen Plugins:**
- Ein Install statt drei
- Geteilte Services (Projekt-Erkennung informiert LSP und Runner)
- Einheitliche Settings-Page
- Einfachere Wartung und Updates

## TODO

- [x] **GDExtension Rust-Navigation:** Strg+Click auf GDExtension-Typen navigiert zur Rust `#[derive(GodotClass)]` Struct via `GdExtensionGotoDeclarationHandler`. Mapping mit Cache und VFS-Invalidierung (`GdExtensionRustResolver`). Unterstuetzt `#[class(rename=...)]`.
- [x] **Mehrzeilige Lambdas in Funktionsargumenten:** Lexer-Fix (`markLambda` mit `atIndent+1`) und Parser-Fixes (`GdStmtParser`, `GdPsiBuilder.mceEndStmt`, `GdArgListParser`) fuer mehrzeilige Lambdas als Funktionsargumente mit trailing commas. Funktioniert wenn `func` am Zeilenanfang steht (innerhalb Klammern). Edge-Case: `func` mitten in einer Zeile (z.B. `other(func():`) noch nicht unterstuetzt (RIDER-126458).

- [x] **SDK-Stubs fuer Godot 4.6+:** SDK-Stubs werden lokal via Kotlin SDK-Builder (`buildSrc/`) gebaut. Support nur fuer Godot 4.6+, aeltere Versionen werden nicht mehr unterstuetzt. Der Builder filtert auf `>= 4.6` Tags.
- [ ] **GDExtension Rust PSI statt Regex:** `GdExtensionRustResolver` von Regex-Parsing auf RustRover Rust PSI API (`org.rust.lang`) umstellen. Vorteile: Type-Alias-Aufloesung, Cross-File/Cross-Crate-Typen, Robustheit bei ungewoehnlicher Formatierung, Macro-Expansion (`#[godot_api]`). Dependency: `org.rust.lang` Plugin (in RustRover gebundelt). Niedrige Prioritaet — aktueller Regex-Ansatz deckt gdext-Standard gut ab.

## Nicht portiert (Rider-spezifisch)

Folgende Features sind an Riders RD-Protocol und .NET-Infrastruktur gebunden und werden nicht portiert:

- C# Code-Analyse und Completions (ReSharper-Backend)
- .NET-Debugging und Remote-Debugging
- Mono/Edit-and-Continue Support
- .NET-Runtime-Autodetection
- RD-Protocol Frontend-Backend-Kommunikation
- C# Unit-Test-Framework-Integration (gdUnit4Net)

## Build

Voraussetzung: JDK 21

```bash
# Branch 253 (fuer RustRover 2025.3)
git checkout 253

# Community Plugin bauen (Dependency)
cd community && ./gradlew buildPlugin && cd ..

# GDScript Plugin bauen (SDK wird automatisch beim ersten Build erstellt, erfordert Internetzugang)
cd gdscript && ./gradlew buildPlugin && cd ..

# Ergebnis: gdscript/build/distributions/rider-gdscript.zip
```

## Kompatibilität

| IDE | Build | Status |
|-----|-------|--------|
| RustRover 2025.3 | 253.31033 | Ziel-Plattform |
| IntelliJ IDEA 2025.3 | 253.x | Sollte funktionieren |
| CLion 2025.3 | 253.x | Sollte funktionieren |
| PyCharm 2025.3 | 253.x | Sollte funktionieren |

**Godot-Version:** 4.6+ (aeltere Versionen werden nicht unterstuetzt)

## Quelle

Basiert auf [JetBrains/godot-support](https://github.com/JetBrains/godot-support), Branch `253`.
