# Plugin-Zusammenfuehrung Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Community + Godot-LSP + GdScript in ein einzelnes Plugin zusammenfuehren — ein Install statt drei.

**Architecture:** Neuen Branch von `253-merge` erstellen, `253` reinmergen, dann community- und godot-lsp-Dateien in gdscript verschieben. Package-Renames durchfuehren, plugin.xml vereinen, alte Module loeschen.

**Tech Stack:** Kotlin, Gradle, IntelliJ Platform SDK

**Voraussetzung:** Branches `253` und `253-merge` muessen aktuell sein.

---

## File Structure

### Dateien die verschoben werden (community → gdscript)

| Quelle (253-merge) | Ziel |
|---|---|
| `community/src/main/kotlin/.../gdscript/GdLanguage.kt` | `gdscript/src/main/kotlin/gdscript/GdLanguage.kt` |
| `community/src/main/kotlin/.../gdscript/GdFileType.kt` | `gdscript/src/main/kotlin/gdscript/GdFileType.kt` |
| `community/src/main/kotlin/.../GodotCommunityBundle.kt` | Inhalte in `GdScriptBundle` mergen |
| `community/src/main/kotlin/.../GodotMetadataService.kt` | `gdscript/src/main/kotlin/project/GodotMetadataService.kt` |
| `community/src/main/kotlin/.../GodotProjectProvider.kt` | `gdscript/src/main/kotlin/project/GodotProjectProvider.kt` |
| `community/src/main/kotlin/.../GodotMetadataFileWatcher.kt` | `gdscript/src/main/kotlin/project/GodotMetadataFileWatcher.kt` |
| `community/src/main/kotlin/.../GodotMetadataFileWatcherManager.kt` | `gdscript/src/main/kotlin/project/GodotMetadataFileWatcherManager.kt` |
| `community/src/main/kotlin/.../GodotEditorConnectionProvider.kt` | `gdscript/src/main/kotlin/project/GodotEditorConnectionProvider.kt` |
| `community/src/main/kotlin/.../GodotMajorVersion.kt` | `gdscript/src/main/kotlin/project/GodotMajorVersion.kt` |
| `community/src/main/kotlin/.../actions/GodotActionsToolbar.kt` | `gdscript/src/main/kotlin/project/actions/GodotActionsToolbar.kt` |
| `community/src/main/kotlin/.../actions/StartGodotEditorAction.kt` | `gdscript/src/main/kotlin/project/actions/StartGodotEditorAction.kt` |
| `community/src/main/kotlin/.../actions/GodotEditorLaunchConfig.kt` | `gdscript/src/main/kotlin/project/actions/GodotEditorLaunchConfig.kt` |
| `community/src/main/kotlin/.../utils/GodotCommunityUtil.kt` | `gdscript/src/main/kotlin/project/utils/GodotCommunityUtil.kt` |
| `community/src/main/kotlin/.../utils/GodotFileUtil.kt` | `gdscript/src/main/kotlin/project/utils/GodotFileUtil.kt` |
| `community/src/main/kotlin/.../run/configurations/GodotRunContextUtil.kt` | `gdscript/src/main/kotlin/project/run/GodotRunContextUtil.kt` |
| `community/src/main/kotlin/.../tscn/TscnFileType.kt` | `gdscript/src/main/kotlin/tscn/TscnFileType.kt` |
| `community/src/main/kotlin/.../tscn/TscnLanguage.kt` | `gdscript/src/main/kotlin/tscn/TscnLanguage.kt` |
| `community/src/main/java/.../RustRoverPluginsGodotCommunityIcons.java` | Referenzen auf gdscript Icons umstellen |
| `community/src/main/resources/icons/*` | `gdscript/src/main/resources/icons/` |
| `community/src/main/resources/messages/GodotCommunityBundle.properties` | Inhalte in `GdScriptBundle.properties` mergen |

### Dateien die verschoben werden (godot-lsp → gdscript)

Auf `253-merge` sind die meisten LSP-Dateien bereits unter `gdscript/src/main/kotlin/gdscript/lsp/`. Was noch im separaten `godot-lsp/` Modul liegt:

| Quelle | Ziel |
|---|---|
| `godot-lsp/src/main/kotlin/godot/lsp/GodotLspBundle.kt` | Inhalte in `GdScriptBundle` mergen |
| `godot-lsp/src/main/kotlin/godot/lsp/GodotLspIcons.kt` | Referenzen auf gdscript Icons umstellen |
| `godot-lsp/src/main/kotlin/godot/lsp/GodotUtil.kt` | Pruefen ob auf 253-merge noch gebraucht |
| `godot-lsp/src/main/kotlin/godot/lsp/settings/*` | `gdscript/src/main/kotlin/gdscript/lsp/settings/` |
| `godot-lsp/src/main/kotlin/godot/lsp/service/*` | Bereits auf 253-merge in gdscript/lsp/ |
| `godot-lsp/src/main/resources/icons/*` | Bereits in community/resources/icons/ (identisch) |
| `godot-lsp/src/main/resources/messages/GodotLspBundle.properties` | Inhalte in `GdScriptBundle.properties` mergen |

### Dateien die geloescht werden

| Datei/Ordner | Grund |
|---|---|
| `community/` | Komplett — alles nach gdscript verschoben |
| `godot-lsp/` | Komplett — alles nach gdscript verschoben |

### Dateien die geaendert werden

| Datei | Aenderung |
|---|---|
| `gdscript/settings.gradle.kts` | `includeBuild` Eintraege entfernen |
| `gdscript/build.gradle.kts` | `compileOnly` Dependencies entfernen, `localPlugin` entfernen |
| `gdscript/src/main/resources/META-INF/plugin.xml` | Extensions aus community + godot-lsp uebernehmen, `<depends>` entfernen |
| Alle Dateien die `com.jetbrains.rustrover.godot.community.*` importieren | Imports anpassen |

---

### Task 1: Branch vorbereiten und 253 in 253-merge mergen

**Files:**
- Modify: git branches

- [ ] **Step 1: Neuen Branch von 253-merge erstellen**

```bash
cd /home/tholo/plugins/godot-support
git checkout 253-merge
git checkout -b 253-unified
```

- [ ] **Step 2: 253 in 253-unified mergen**

```bash
git merge 253 --no-edit
```

Erwartet: Merge-Konflikte wegen rider→rustrover Renames. Die Konflikte betreffen hauptsaechlich:
- Import-Statements: `com.jetbrains.rider.` → `com.jetbrains.rustrover.`
- Icon-Klassen: `RiderPlugins...` → `RustRoverPlugins...`
- Plugin IDs: `rider.godot` → `rustrover.godot`

- [ ] **Step 3: Merge-Konflikte loesen**

Fuer jeden Konflikt: Die `253-merge` Version (rustrover) als Basis nehmen und unsere inhaltlichen Aenderungen aus `253` uebernehmen. Konkret:
- Package-Namen: immer `rustrover` verwenden (nicht `rider`)
- Plugin IDs: immer `rustrover.godot` verwenden
- Icons: immer `RustRoverPlugins...` verwenden
- Inhaltliche Aenderungen (SDK-Builder, GdExtension-Fixes, Parser-Fixes, Tests): vollstaendig uebernehmen

```bash
# Nach dem Loesen aller Konflikte:
git add -A
git commit --no-edit
```

- [ ] **Step 4: Build verifizieren**

```bash
cd community && ./gradlew buildPlugin 2>&1 | tail -3
cd ../gdscript && ./gradlew compileKotlin 2>&1 | tail -3
```

Erwartet: Beide `BUILD SUCCESSFUL`

- [ ] **Step 5: Tests laufen lassen**

```bash
cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test 2>&1 | tail -10
```

Erwartet: Alle Tests gruen (oder dieselben Failures wie auf `253`)

- [ ] **Step 6: Commit falls Fixes noetig waren**

```bash
git add -A
git commit -m "fix: resolve merge conflicts from 253 into 253-unified"
```

---

### Task 2: GodotCommunityBundle in GdScriptBundle mergen

**Files:**
- Modify: `gdscript/src/main/resources/messages/GdScriptBundle.properties`
- Delete (spaeter): `community/src/main/resources/messages/GodotCommunityBundle.properties`

- [ ] **Step 1: Community Bundle-Eintraege in GdScriptBundle uebernehmen**

Am Ende von `gdscript/src/main/resources/messages/GdScriptBundle.properties` hinzufuegen:

```properties
# Godot Community (merged from GodotCommunityBundle)
language.file_name=GdScript file
filetype.tscn.file.description=Godot's scene file
group.GodotActionsToolbar.text=Godot Toolbar
action.StartGodotEditorAction.text=Start Godot Editor
connected.to.godot.editor.text=Connected to Godot Editor
not.connected.to.godot.editor.text=Not Connected to Godot Editor
godot.toolbar.text=Godot
```

Hinweis: `language.name=GDScript` existiert schon in GdScriptBundle (mit grossem D). Die Community-Version `language.name=GdScript` (kleines d) wird NICHT uebernommen — GdScriptBundle hat Vorrang.

- [ ] **Step 2: Pruefen ob GodotLspBundle Eintraege hat die noch fehlen**

```bash
cat godot-lsp/src/main/resources/messages/GodotLspBundle.properties
```

Falls Eintraege vorhanden, diese ebenfalls in GdScriptBundle uebernehmen.

- [ ] **Step 3: Build kompiliert**

```bash
cd /home/tholo/plugins/godot-support/gdscript && ./gradlew compileKotlin 2>&1 | tail -3
```

Erwartet: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add gdscript/src/main/resources/messages/GdScriptBundle.properties
git commit -m "feat: merge GodotCommunityBundle and GodotLspBundle into GdScriptBundle"
```

---

### Task 3: Community-Dateien nach gdscript verschieben

**Files:**
- Create: `gdscript/src/main/kotlin/project/` Verzeichnis mit Unterordnern
- Move: Alle community Kotlin-Dateien (siehe File Structure oben)
- Move: Icons aus community nach gdscript

- [ ] **Step 1: Zielverzeichnisse erstellen**

```bash
cd /home/tholo/plugins/godot-support
mkdir -p gdscript/src/main/kotlin/project/actions
mkdir -p gdscript/src/main/kotlin/project/utils
mkdir -p gdscript/src/main/kotlin/project/run
```

- [ ] **Step 2: GdLanguage und GdFileType nach gdscript/ verschieben**

```bash
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/gdscript/GdLanguage.kt gdscript/src/main/kotlin/gdscript/GdLanguage.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/gdscript/GdFileType.kt gdscript/src/main/kotlin/gdscript/GdFileType.kt
```

Package in beiden Dateien aendern:
```kotlin
// Vorher:
package com.jetbrains.rustrover.godot.community.gdscript

// Nachher:
package gdscript
```

In `GdLanguage.kt` den Bundle-Aufruf anpassen:
```kotlin
// Vorher:
import com.jetbrains.rustrover.godot.community.GodotCommunityBundle
// ...
override fun getDisplayName(): String = GodotCommunityBundle.message("language.name")

// Nachher:
import GdScriptBundle
// ...
override fun getDisplayName(): String = GdScriptBundle.message("language.name")
```

In `GdFileType.kt` den Bundle- und Icon-Aufruf anpassen:
```kotlin
// Vorher:
import com.intellij.rustrover.plugins.godot.community.icons.RustRoverPluginsGodotCommunityIcons
import com.jetbrains.rustrover.godot.community.GodotCommunityBundle
// ...
override fun getDescription(): String = GodotCommunityBundle.message("language.file_name")
override fun getIcon(): Icon = RustRoverPluginsGodotCommunityIcons.GDScript

// Nachher:
import GdScriptBundle
import GdScriptIcons
// ...
override fun getDescription(): String = GdScriptBundle.message("language.file_name")
override fun getIcon(): Icon = GdScriptIcons.GD_FILE
```

Hinweis: `GdScriptIcons` existiert bereits im gdscript-Modul. Pruefen welche Konstante das GDScript-Icon referenziert und ggf. anpassen.

- [ ] **Step 3: Project-Dateien verschieben**

```bash
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/GodotMetadataService.kt gdscript/src/main/kotlin/project/GodotMetadataService.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/GodotProjectProvider.kt gdscript/src/main/kotlin/project/GodotProjectProvider.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/GodotMetadataFileWatcher.kt gdscript/src/main/kotlin/project/GodotMetadataFileWatcher.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/GodotMetadataFileWatcherManager.kt gdscript/src/main/kotlin/project/GodotMetadataFileWatcherManager.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/GodotEditorConnectionProvider.kt gdscript/src/main/kotlin/project/GodotEditorConnectionProvider.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/GodotMajorVersion.kt gdscript/src/main/kotlin/project/GodotMajorVersion.kt
```

In jeder Datei das Package aendern:
```kotlin
// Vorher:
package com.jetbrains.rustrover.godot.community

// Nachher:
package project
```

Und alle Imports von `com.jetbrains.rustrover.godot.community.*` auf die neuen Packages aktualisieren.

- [ ] **Step 4: Actions verschieben**

```bash
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/actions/GodotActionsToolbar.kt gdscript/src/main/kotlin/project/actions/GodotActionsToolbar.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/actions/StartGodotEditorAction.kt gdscript/src/main/kotlin/project/actions/StartGodotEditorAction.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/actions/GodotEditorLaunchConfig.kt gdscript/src/main/kotlin/project/actions/GodotEditorLaunchConfig.kt
```

Package aendern:
```kotlin
// Vorher:
package com.jetbrains.rustrover.godot.community.actions

// Nachher:
package project.actions
```

- [ ] **Step 5: Utils verschieben**

```bash
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/utils/GodotCommunityUtil.kt gdscript/src/main/kotlin/project/utils/GodotCommunityUtil.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/utils/GodotFileUtil.kt gdscript/src/main/kotlin/project/utils/GodotFileUtil.kt
```

Package aendern:
```kotlin
// Vorher:
package com.jetbrains.rustrover.godot.community.utils

// Nachher:
package project.utils
```

- [ ] **Step 6: Run-Config und Tscn verschieben**

```bash
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/run/configurations/GodotRunContextUtil.kt gdscript/src/main/kotlin/project/run/GodotRunContextUtil.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/tscn/TscnFileType.kt gdscript/src/main/kotlin/tscn/TscnFileType.kt
cp community/src/main/kotlin/com/jetbrains/rustrover/godot/community/tscn/TscnLanguage.kt gdscript/src/main/kotlin/tscn/TscnLanguage.kt
```

Packages aendern:
- `GodotRunContextUtil.kt`: `package project.run`
- `TscnFileType.kt`: `package tscn`
- `TscnLanguage.kt`: `package tscn`

- [ ] **Step 7: Icons verschieben**

```bash
cp community/src/main/resources/icons/Godot.svg gdscript/src/main/resources/icons/
cp community/src/main/resources/icons/Godot_dark.svg gdscript/src/main/resources/icons/
cp community/src/main/resources/icons/GodotDisconnected.svg gdscript/src/main/resources/icons/
cp community/src/main/resources/icons/GodotDisconnected_dark.svg gdscript/src/main/resources/icons/
cp community/src/main/resources/icons/TscnFile.svg gdscript/src/main/resources/icons/
```

GDScript.svg sollte schon in gdscript existieren, pruefen und ggf. kopieren.

- [ ] **Step 8: Commit**

```bash
cd /home/tholo/plugins/godot-support
git add gdscript/src/main/kotlin/project/ gdscript/src/main/kotlin/gdscript/GdLanguage.kt gdscript/src/main/kotlin/gdscript/GdFileType.kt gdscript/src/main/kotlin/tscn/TscnFileType.kt gdscript/src/main/kotlin/tscn/TscnLanguage.kt gdscript/src/main/resources/icons/
git commit -m "feat: move community sources into gdscript module"
```

---

### Task 4: Godot-LSP restliche Dateien verschieben

Auf `253-merge` sind die meisten LSP-Dateien schon unter `gdscript/lsp/`. Was noch im separaten `godot-lsp/` liegt muss rein.

**Files:**
- Move: godot-lsp Settings nach `gdscript/src/main/kotlin/gdscript/lsp/settings/`
- Move: godot-lsp Util/Icons falls noch fehlend

- [ ] **Step 1: Pruefen was noch im godot-lsp Modul liegt und in gdscript fehlt**

```bash
# Dateien in godot-lsp:
find godot-lsp/src -name "*.kt" | sort

# Dateien in gdscript/lsp:
find gdscript/src/main/kotlin/gdscript/lsp -name "*.kt" | sort
```

Vergleichen und fehlende Dateien identifizieren. Auf `253-merge` fehlen wahrscheinlich:
- `settings/GodotLspSettings.kt`
- `settings/GodotLspSettingsConfigurable.kt`
- `GodotUtil.kt`

- [ ] **Step 2: Fehlende Dateien verschieben**

```bash
mkdir -p gdscript/src/main/kotlin/gdscript/lsp/settings
cp godot-lsp/src/main/kotlin/godot/lsp/settings/GodotLspSettings.kt gdscript/src/main/kotlin/gdscript/lsp/settings/
cp godot-lsp/src/main/kotlin/godot/lsp/settings/GodotLspSettingsConfigurable.kt gdscript/src/main/kotlin/gdscript/lsp/settings/
cp godot-lsp/src/main/kotlin/godot/lsp/GodotUtil.kt gdscript/src/main/kotlin/gdscript/lsp/
```

Packages aendern:
```kotlin
// Vorher:
package godot.lsp.settings
// oder:
package godot.lsp

// Nachher:
package gdscript.lsp.settings
// oder:
package gdscript.lsp
```

Imports auf `GodotLspBundle` durch `GdScriptBundle` ersetzen.

- [ ] **Step 3: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/lsp/
git commit -m "feat: move remaining godot-lsp sources into gdscript module"
```

---

### Task 5: Alle Imports im gdscript-Modul aktualisieren

**Files:**
- Modify: Alle Dateien die `com.jetbrains.rustrover.godot.community.*` importieren (~34 Dateien)

- [ ] **Step 1: Alle betroffenen Imports finden**

```bash
cd /home/tholo/plugins/godot-support
grep -rn "com.jetbrains.rustrover.godot.community" gdscript/src/ --include="*.kt" | sort
```

- [ ] **Step 2: Imports ersetzen**

Systematisches Ersetzen in allen Kotlin-Dateien unter `gdscript/src/`:

```
com.jetbrains.rustrover.godot.community.gdscript.GdLanguage  →  gdscript.GdLanguage
com.jetbrains.rustrover.godot.community.gdscript.GdFileType  →  gdscript.GdFileType
com.jetbrains.rustrover.godot.community.GodotProjectProvider  →  project.GodotProjectProvider
com.jetbrains.rustrover.godot.community.GodotEditorConnectionProvider  →  project.GodotEditorConnectionProvider
com.jetbrains.rustrover.godot.community.GodotMajorVersion  →  project.GodotMajorVersion
com.jetbrains.rustrover.godot.community.GodotMetadataService  →  project.GodotMetadataService
com.jetbrains.rustrover.godot.community.GodotCommunityBundle  →  GdScriptBundle
com.jetbrains.rustrover.godot.community.utils.GodotCommunityUtil  →  project.utils.GodotCommunityUtil
com.jetbrains.rustrover.godot.community.utils.GodotFileUtil  →  project.utils.GodotFileUtil
com.jetbrains.rustrover.godot.community.tscn.TscnFileType  →  tscn.TscnFileType
com.jetbrains.rustrover.godot.community.tscn.TscnLanguage  →  tscn.TscnLanguage
com.jetbrains.rustrover.godot.community.actions.*  →  project.actions.*
com.jetbrains.rustrover.godot.community.run.*  →  project.run.*
```

Icon-Referenzen:
```
com.intellij.rustrover.plugins.godot.community.icons.RustRoverPluginsGodotCommunityIcons  →  GdScriptIcons (oder passende Icon-Klasse)
```

Auch Imports von `godot.lsp.*` pruefen und auf `gdscript.lsp.*` umstellen falls noetig.

```bash
find gdscript/src -name "*.kt" -exec grep -l "com.jetbrains.rustrover.godot.community" {} \; | xargs sed -i 's/com\.jetbrains\.rustrover\.godot\.community\.gdscript\.GdLanguage/gdscript.GdLanguage/g'
# ... (analog fuer alle anderen Imports)
```

- [ ] **Step 3: Build kompiliert**

```bash
cd /home/tholo/plugins/godot-support/gdscript && ./gradlew compileKotlin 2>&1 | tail -10
```

Erwartet: `BUILD SUCCESSFUL` (ggf. Warnings, aber keine Errors)

- [ ] **Step 4: Commit**

```bash
cd /home/tholo/plugins/godot-support
git add gdscript/src/
git commit -m "refactor: update all imports from community/godot-lsp to unified packages"
```

---

### Task 6: plugin.xml vereinen

**Files:**
- Modify: `gdscript/src/main/resources/META-INF/plugin.xml`

- [ ] **Step 1: Community-Extensions in gdscript plugin.xml uebernehmen**

In `gdscript/src/main/resources/META-INF/plugin.xml` folgende Aenderungen:

1. **Extension Points hinzufuegen** (aus community plugin.xml):

```xml
  <extensionPoints>
    <extensionPoint name="godotProjectProvider" interface="project.GodotProjectProvider" dynamic="true" />
    <extensionPoint name="editorConnectionProvider" interface="project.GodotEditorConnectionProvider" dynamic="true"/>
  </extensionPoints>
```

2. **FileType-Registrierungen hinzufuegen** (aus community):

```xml
    <fileType name="GdScript" implementationClass="gdscript.GdFileType" fieldName="INSTANCE" language="GdScript" extensions="gd;gdf"/>
    <fileType name="Tscn file" implementationClass="tscn.TscnFileType" fieldName="INSTANCE" language="Tscn" extensions="tscn"/>
    <postStartupActivity implementation="project.GodotMetadataFileWatcherManager"/>
```

3. **Actions hinzufuegen** (aus community):

```xml
  <actions>
    <!-- bestehende gdscript actions ... -->
    <group id="GodotActionsToolbar" popup="true" class="project.actions.GodotActionsToolbar" icon="GdScriptIcons.Godot">
      <add-to-group group-id="MainToolbarRight" anchor="first" />
      <action id="StartGodotEditorAction"
              class="project.actions.StartGodotEditorAction"
              icon="AllIcons.Actions.Execute">
      </action>
    </group>
  </actions>
```

4. **LSP-Settings hinzufuegen** (aus godot-lsp, falls nicht schon vorhanden):

```xml
    <applicationService
        serviceImplementation="gdscript.lsp.settings.GodotLspSettings"/>
    <applicationConfigurable
        instance="gdscript.lsp.settings.GodotLspSettingsConfigurable"
        id="godot.lsp.settings"
        displayName="Godot LSP"
        parentId="tools"/>
```

- [ ] **Step 2: Dependencies bereinigen**

In `plugin.xml`:

```xml
<!-- ENTFERNEN: -->
<plugin id="com.intellij.rustrover.godot.community"/>

<!-- ENTFERNEN: -->
<depends optional="true" config-file="gdext-stub-support.xml">godot.lsp</depends>
```

Die `godot.lsp` optional dependency entfernen — der LSP-Code ist jetzt direkt im Plugin. Den Inhalt von `gdext-stub-support.xml` direkt in die Haupt-`plugin.xml` verschieben.

```bash
cat gdscript/src/main/resources/META-INF/gdext-stub-support.xml
```

Dessen Inhalt in die Haupt-plugin.xml integrieren und die Datei loeschen.

- [ ] **Step 3: Resource-Bundle pruefen**

Sicherstellen dass nur ein `resource-bundle` deklariert ist:

```xml
<resource-bundle>messages.GdScriptBundle</resource-bundle>
```

- [ ] **Step 4: Build kompiliert**

```bash
cd /home/tholo/plugins/godot-support/gdscript && ./gradlew compileKotlin 2>&1 | tail -5
```

Erwartet: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add gdscript/src/main/resources/META-INF/
git commit -m "feat: unify plugin.xml — merge community and godot-lsp extensions"
```

---

### Task 7: Build-System bereinigen

**Files:**
- Modify: `gdscript/settings.gradle.kts`
- Modify: `gdscript/build.gradle.kts`

- [ ] **Step 1: settings.gradle.kts bereinigen**

In `gdscript/settings.gradle.kts` die `includeBuild` Eintraege entfernen:

```kotlin
// Vorher:
rootProject.name = "rustrover-gdscript"

includeBuild("../community")
includeBuild("../godot-lsp")
include("sdkBuilder")

// Nachher:
rootProject.name = "rustrover-gdscript"

include("sdkBuilder")
```

- [ ] **Step 2: build.gradle.kts bereinigen**

In `gdscript/build.gradle.kts`:

```kotlin
// ENTFERNEN:
compileOnly(":rustrover-godot-community")
compileOnly(":godot-lsp")

// ENTFERNEN:
localPlugin(repoRoot.resolve("community/build/distributions/rustrover-godot-community.zip"))
```

- [ ] **Step 3: Build kompiliert**

```bash
cd /home/tholo/plugins/godot-support/gdscript && ./gradlew compileKotlin 2>&1 | tail -5
```

Erwartet: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add gdscript/settings.gradle.kts gdscript/build.gradle.kts
git commit -m "chore: remove community and godot-lsp build dependencies"
```

---

### Task 8: Alte Module loeschen

**Files:**
- Delete: `community/`
- Delete: `godot-lsp/`

- [ ] **Step 1: Community-Modul loeschen**

```bash
cd /home/tholo/plugins/godot-support
git rm -r community/
```

- [ ] **Step 2: Godot-LSP-Modul loeschen**

```bash
git rm -r godot-lsp/
```

- [ ] **Step 3: Build verifizieren**

```bash
cd gdscript && ./gradlew buildPlugin 2>&1 | tail -5
```

Erwartet: `BUILD SUCCESSFUL`

- [ ] **Step 4: Plugin-ZIP verifizieren**

```bash
zipinfo gdscript/build/distributions/rustrover-gdscript.zip | grep -E "\.jar|\.svg|sdk" | head -15
```

Erwartet: Ein JAR das alle Klassen enthaelt, Icons vorhanden, SDK vorhanden.

- [ ] **Step 5: Pruefen dass community-Klassen im JAR sind**

```bash
cd /tmp && rm -rf check-jar && mkdir check-jar && cd check-jar
unzip /home/tholo/plugins/godot-support/gdscript/build/distributions/rustrover-gdscript.zip
jar tf rustrover-gdscript/lib/rustrover-gdscript.jar | grep -E "GdLanguage|GdFileType|GodotMetadata|GodotLsp" | head -10
```

Erwartet: Alle verschobenen Klassen im JAR.

- [ ] **Step 6: Commit**

```bash
cd /home/tholo/plugins/godot-support
git add -A
git commit -m "chore: remove community and godot-lsp modules (merged into gdscript)"
```

---

### Task 9: Tests ausfuehren und README aktualisieren

**Files:**
- Modify: `Plugin-README.md`

- [ ] **Step 1: Alle Tests laufen lassen**

```bash
cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test 2>&1 | tail -20
```

Erwartet: Alle Tests gruen. Falls Tests fehlschlagen die community-Klassen referenzieren, Imports in den Test-Dateien anpassen.

- [ ] **Step 2: Plugin-README Build-Anleitung aktualisieren**

In `Plugin-README.md` den Build-Abschnitt aendern:

```markdown
## Build

Voraussetzung: JDK 21

\```bash
# Branch 253-unified (fuer RustRover 2025.3)
git checkout 253-unified

# Plugin bauen (SDK wird automatisch beim ersten Build erstellt, erfordert Internetzugang)
cd gdscript && ./gradlew buildPlugin && cd ..

# Ergebnis: gdscript/build/distributions/rustrover-gdscript.zip
\```
```

Kein separater Community-Build mehr noetig!

- [ ] **Step 3: Plugin-Zusammenfuehrung TODO als erledigt markieren**

Falls der TODO-Eintrag noch in der README steht (wurde in der Brainstorming-Session entfernt und durch Spec-Verweis ersetzt), pruefen und ggf. aktualisieren.

- [ ] **Step 4: Commit**

```bash
git add Plugin-README.md
git commit -m "docs: update build instructions for unified plugin"
```

---

### Task 10: End-to-End Verifikation

- [ ] **Step 1: Clean Build**

```bash
cd /home/tholo/plugins/godot-support/gdscript && ./gradlew clean buildPlugin 2>&1 | tail -10
```

Erwartet: `BUILD SUCCESSFUL`

- [ ] **Step 2: SDK im Plugin**

```bash
tar -tf build/sdk/sdk.tar.xz | cut -d/ -f1 | sort -u
```

Erwartet: `4.6`, `4.6.1`, `Master` — keine alten Versionen.

- [ ] **Step 3: Plugin-Inhalt pruefen**

```bash
zipinfo build/distributions/rustrover-gdscript.zip | head -20
```

Erwartet: Ein JAR, SDK, Icons — alles in einem Plugin.

- [ ] **Step 4: Keine Referenzen auf alte Module**

```bash
grep -r "rider-godot-community\|rustrover-godot-community\|godot\.lsp" gdscript/src/ --include="*.kt" --include="*.xml" | grep -v "//\|notification\.group\|lsp\." | head -10
```

Erwartet: Keine Treffer (ausser in Kommentaren oder notification group IDs).
