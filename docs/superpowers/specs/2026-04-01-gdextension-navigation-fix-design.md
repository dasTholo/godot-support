# GDExtension Navigation Fix: Rust statt Stub

**Datum:** 2026-04-01
**Status:** Approved

## Problem

Ctrl+Click auf eine GDExtension-Klasse im GDScript (z.B. `MyRustClass` in `extends MyRustClass` oder `var x: MyRustClass`) navigiert zum generierten SDK-Stub (`.gd` Datei in `gdext-stubs/`) statt zur Rust-Struct mit `#[derive(GodotClass)]`.

### Ursache

IntelliJ Platform's Go-to-Declaration-Flow hat zwei getrennte Mechanismen:

1. **`PsiReference.resolve()`** — wird zuerst aufgerufen, direkt vom PSI-Element
2. **`GotoDeclarationHandler`** — wird danach als Alternative aufgerufen

Der bestehende `GdExtensionGotoDeclarationHandler` ist als `GotoDeclarationHandler` mit `order="first"` registriert. Das wirkt aber nur relativ zu anderen Handlern, nicht gegenüber dem `PsiReference`-Pfad. Die Resolution-Kette:

```
Ctrl+Click auf GDExtension-Klassenname
  → GdTypeHintRef.getReferences()  (via GdRefElementImpl → ReferenceProvidersRegistry)
    → GdTypeHintReferenceContributor liefert GdTypeHintReference
      → resolve() findet .gd Stub via GdClassNamingIndex
        → IDE navigiert zum Stub
```

Der `GdExtensionGotoDeclarationHandler` kommt nie zum Zug.

## Ansatz: Reference-Resolve abfangen

Die Resolution direkt auf PsiReference-Ebene abfangen — dort wo sie tatsächlich passiert.

### Alternativen (verworfen)

- **PsiReferenceContributor mit hoher Prioritaet:** Nicht zuverlaessig steuerbar wenn mehrere Contributors auf dasselbe Element matchen; potentielle Konflikte.
- **GotoDeclarationHandler + Workaround:** Kaempft gegen die Platform-Architektur; fragil bei Platform-Updates.

## Design

### 1. Utility-Funktion in `GdExtensionRustResolver`

Neue Methode `resolveToRustIfStub(resolved: PsiElement?, project: Project): PsiElement?`:

- Prueft ob `resolved` in einem GDExtension-Stub liegt (Pfad enthaelt `/gdext-stubs/`)
- Wenn ja: Extrahiert den Klassennamen aus dem aufgeloesten Element
- Schlaegt im `GdExtensionRustResolver.buildClassNameMapping()` nach
- Gibt das Rust-PsiElement zurueck (struct-Definition im `.rs`-File)
- Wenn nein oder kein Mapping vorhanden: gibt das Original `resolved` zurueck

### 2. `GdTypeHintReference.resolve()` modifizieren

In `gdscript/src/main/kotlin/gdscript/reference/GdTypeHintReference.kt`, Zeile 56:

```kotlin
// Vorher:
val classId = GdClassNamingIndex.INSTANCE.getGlobally(key, project).firstOrNull()?.classNameNmi
if (classId != null) return@Resolver classId

// Nachher:
val classId = GdClassNamingIndex.INSTANCE.getGlobally(key, project).firstOrNull()?.classNameNmi
if (classId != null) {
    return@Resolver GdExtensionRustResolver.resolveToRustIfStub(classId, project)
}
```

### 3. `GdInheritanceReference.resolve()` modifizieren

In `gdscript/src/main/kotlin/gdscript/reference/GdInheritanceReference.kt`, Zeile 57:

```kotlin
// Vorher:
GdClassUtil.getClassIdElement(key, element, project)

// Nachher:
GdClassUtil.getClassIdElement(key, element, project)?.let {
    GdExtensionRustResolver.resolveToRustIfStub(it, project)
}
```

### 4. `GdExtensionGotoDeclarationHandler` entfernen

- Datei `gdscript/src/main/kotlin/gdscript/extension/GdExtensionGotoDeclarationHandler.kt` loeschen
- `gotoDeclarationHandler`-Eintrag aus `plugin.xml` (Zeile 251) entfernen

### Erkennung eines Stubs

Ueber den Dateipfad des aufgeloesten Elements:

```kotlin
resolved.containingFile?.virtualFile?.path?.contains("/gdext-stubs/") == true
```

Dies ist robust, da der Pfad `PathManager.getConfigDir()/gdext-stubs/<projectHash>` stabil ist und ausschliesslich generierte Stubs enthaelt.

### Klassennamen-Extraktion

Das aufgeloeste Element ist ein `classNameNmi` (der Name in `class_name Foo`). Der Text dieses Elements ist direkt der Klassenname, der als Key fuer `buildClassNameMapping()` dient.

## Nicht im Scope

- Aenderungen an der Stub-Generierung
- Neue UI-Elemente oder Popups
- Aenderungen am `GdExtensionRustResolver` Cache-Mechanismus
