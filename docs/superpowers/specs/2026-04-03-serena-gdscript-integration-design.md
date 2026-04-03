# Serena GDScript Integration via godot-support Plugin

**Datum:** 2026-04-03  
**Status:** Genehmigt  
**Repo:** `dasTholo/godot-support`  
**Arbeitsverzeichnis:** `/home/tholo/plugins/godot-support`  
**Plugin-Version:** 0.26.0

## Zusammenfassung

GDScript-Symbole sollen über die vorhandenen Serena JetBrains-Tools (`find_symbol`, `find_referencing_symbols`, `get_symbols_overview`, `rename_symbol`, `get_supertypes`, `get_subtypes`) nutzbar werden. Dazu muss das godot-support Plugin um `GotoSymbolContributor`, `GotoClassContributor` und `TypeHierarchyProvider` erweitert werden.

## Problem

Serena nutzt das JetBrains-Backend (HTTP-API des Serena JetBrains Plugins). Dieses Plugin verwendet intern die IntelliJ Platform APIs:

| Serena Endpoint | IntelliJ API | GDScript Status |
|---|---|---|
| `FindSymbolHandler` | `ChooseByNameContributor` (Go to Symbol) | ❌ Fehlt |
| `FindReferencesHandler` | `findSymbol` + `findUsagesProvider` | ⏳ Blockiert durch fehlendes `FindSymbolHandler` |
| `RenameSymbolHandler` | `findSymbol` + `renamePsiElementProcessor` | ⏳ Blockiert durch fehlendes `FindSymbolHandler` |
| `GetSymbolsOverviewHandler` | `StructureView` / `FileStructure` | ✅ Funktioniert |
| `RefreshFileHandler` | Generisch | ✅ Funktioniert |
| `GetSupertypesHandler` | `TypeHierarchyProvider` | ❌ Fehlt |
| `GetSubtypesHandler` | `TypeHierarchyProvider` | ❌ Fehlt |
| `Symbol.getDocumentation()` | `DocumentationProvider` | ✅ Registriert |
| `Symbol.getQuickInfo()` | `DocumentationProvider` | ✅ Registriert |

**Ursache:** Das godot-support Plugin registriert weder `gotoSymbolContributor` noch `typeHierarchyProvider` Extension Points. Daher findet die IntelliJ "Go to Symbol"-Suche keine GDScript-Symbole, die Type Hierarchy zeigt keine GDScript-Klassen, und das Serena Plugin kann diese nicht auflösen.

## Lösung

Drei neue Extension Points im godot-support Plugin implementieren.

### Vorhandene Infrastruktur im godot-support Plugin

**Stub-Indices** (bereits registriert in `plugin.xml`):
- `GdMethodDeclIndex` → `GdMethodDeclTl` (Methoden)
- `GdClassVarDeclIndex` → `GdClassVarDeclTl` (Klassenvariablen)
- `GdConstDeclIndex` → `GdConstDeclTl` (Konstanten)
- `GdEnumDeclIndex` → `GdEnumDeclTl` (Enums)
- `GdSignalDeclIndex` → `GdSignalDeclTl` (Signals)
- `GdClassNamingIndex` → `GdClassNaming` (class_name Deklarationen)
- `GdClassDeclIndex` → `GdClassDeclTl` (innere Klassen)

**Vererbungsinfrastruktur:**
- `GdInheritanceUtil.getExtendedElement()` — löst `extends` zur Elternklasse auf
- `GdInheritanceUtil.getExtendedClassId()` — liefert den Elternklassen-Namen
- `GdInheritanceUtil.isExtending()` — prüft rekursiv mit Zyklen-Erkennung
- `GdExtensionRustResolver.getBaseClass()` — liefert die `base`-Klasse aus Rust `#[class(base = Node2D)]` Annotationen

**Bereits registrierte Extension Points:**
- `lang.findUsagesProvider` → `GdUsageProvider`
- `findUsagesHandlerFactory` → `GdFindUsageHandlerFactory`
- `renamePsiElementProcessor` → `GdRenamePsiFileProcessor`
- `lang.psiStructureViewFactory` → `GdStructureViewFactory`
- `lang.documentationProvider` → `GdDocumentationProvider`

### Zu implementieren

#### 1. `GdGotoSymbolContributor`

**Datei:** `gdscript/src/main/kotlin/gdscript/symbol/GdGotoSymbolContributor.kt`

Implementiert `ChooseByNameContributorEx`. Aggregiert alle Symbol-Indices und liefert `NavigationItem`s für:

- Methoden (`GdMethodDeclIndex`)
- Klassenvariablen (`GdClassVarDeclIndex`)
- Konstanten (`GdConstDeclIndex`)
- Enums (`GdEnumDeclIndex`)
- Signals (`GdSignalDeclIndex`)
- Innere Klassen (`GdClassDeclIndex`)

**API-Vertrag:**
```kotlin
class GdGotoSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?)
    override fun processElementsWithName(name: String, processor: Processor<in NavigationItem>, parameters: FindSymbolParameters)
}
```

Die PSI-Typen (`GdMethodDeclTl`, `GdClassVarDeclTl`, etc.) implementieren bereits `NavigatablePsiElement`, was die `NavigationItem`-Anforderung erfüllt.

#### 2. `GdGotoClassContributor`

**Datei:** `gdscript/src/main/kotlin/gdscript/symbol/GdGotoClassContributor.kt`

Implementiert `ChooseByNameContributorEx` für "Go to Class"-Navigation. Nutzt:
- `GdClassNamingIndex` (class_name Deklarationen)
- `GdClassDeclIndex` (innere Klassen)

#### 3. `GdTypeHierarchyProvider`

**Dateien:**
- `gdscript/src/main/kotlin/gdscript/hierarchy/GdTypeHierarchyProvider.kt`
- `gdscript/src/main/kotlin/gdscript/hierarchy/GdTypeHierarchyBrowser.kt`
- `gdscript/src/main/kotlin/gdscript/hierarchy/GdTypeHierarchyTreeStructure.kt`

Implementiert `TypeHierarchyProvider` für GDScript-Klassen.

**Supertypes:** Nutzt `GdInheritanceUtil.getExtendedElement()` — läuft die `extends`-Kette hoch. Integriert `GdExtensionRustResolver.getBaseClass()` für GDExtension-Klassen (Rust-Structs mit `#[derive(GodotClass)]`).

**Subtypes:** Iteriert über alle Klassen (`GdClassNamingIndex` + `GdClassDeclIndex`) und prüft, ob deren `parent()` dem Ziel entspricht. O(n) ohne Cache — bei Bedarf kann ein Inverse-Index (`parentName → Set<childName>`) nachgerüstet werden.

**Rust-Integration:** Wenn eine GDScript-Klasse eine GDExtension-Klasse erweitert, wird die Rust-Struct `base`-Klasse (`#[class(base = Node2D)]`) in die Hierarchie einbezogen via `GdExtensionRustResolver.getBaseClass(className)`.

#### 4. plugin.xml Registrierung

```xml
<!-- Go to Symbol / Go to Class / Type Hierarchy (enables Serena symbol resolution) -->
<gotoSymbolContributor implementation="gdscript.symbol.GdGotoSymbolContributor"/>
<gotoClassContributor implementation="gdscript.symbol.GdGotoClassContributor"/>
<typeHierarchyProvider language="GDScript" implementationClass="gdscript.hierarchy.GdTypeHierarchyProvider"/>
```

#### 5. Plugin-Version

Das gdscript RustRover Plugin wird auf Version **0.26.0** aktualisiert.

#### 6. Vereinfachung `GdExtensionStubWriter.collectInheritedNames()`

`GdExtensionStubWriter.collectInheritedNames()` (Zeilen 94-128) traversiert die Vererbungskette manuell per Text-Parsing der SDK `.gd`-Dateien auf dem Dateisystem. Das reimplementiert Logik, die in `GdInheritanceUtil` und `GdClassMemberUtil.collectFromParents()` bereits auf PSI/Index-Ebene existiert.

**Aktuell (Text-Parsing):**
```kotlin
// ~35 Zeilen manuelles Parsen von extends, func, var, signal aus .gd-Dateien
private fun collectInheritedNames(baseClass: String, sdkPath: Path): Set<String>
```

**Vereinfacht (PSI/Index):**
Ersetzen durch PSI-basierte Abfrage via `GdClassMemberUtil.collectFromParents()` oder direkte Index-Lookups. Die SDK-Stubs sind zum Zeitpunkt der GDExtension-Stub-Generierung bereits indexiert (SDK wird zuerst als Library registriert).

**Vorteil:** Weniger duplizierter Code, konsistente Vererbungsauflösung über das gesamte Plugin.

### Verifikation

Nach dem Build und der Installation des Plugins in RustRover:

1. **IDE-Test:** `Navigate → Symbol` (Ctrl+Alt+Shift+N) → GDScript-Methoden/Variablen sollten erscheinen
2. **IDE-Test:** `Navigate → Class` (Ctrl+N) → GDScript class_name Deklarationen sollten erscheinen
3. **IDE-Test:** `Navigate → Type Hierarchy` (Ctrl+H) auf einer GDScript-Klasse → Vererbungshierarchie sollte erscheinen
4. **Serena-Test:**
   - `find_symbol` mit `name_path_pattern` → sollte GDScript-Symbole finden
   - `find_referencing_symbols` → sollte Referenzen liefern
   - `rename_symbol` → sollte umbenennen
   - `get_supertypes` / `get_subtypes` → sollte Vererbungshierarchie zeigen

### Build & Deploy

```bash
cd /home/tholo/plugins/godot-support
./gradlew :gdscript:buildPlugin
# Plugin ZIP unter gdscript/build/distributions/
# Installation: RustRover → Settings → Plugins → Install from Disk
```

## Risiken & Einschränkungen

- **PSI-Typen müssen `NavigatablePsiElement` implementieren** — die StructureView bestätigt, dass sie das tun (`GdStructureViewElement` castet sie zu `NavigatablePsiElement`)
- **Serena `SymbolFinder.findSymbolsByName` nutzt `ChooseByNameContributor`** — verifiziert durch Dekompilierung des Serena JB Plugins
- **`find_referencing_symbols` hängt von `find_symbol` ab** — die `FindReferencesHandler` Klasse ruft intern `SymbolFinder.findSymbolByNamePath` auf. Sobald `find_symbol` funktioniert, funktioniert auch `find_referencing_symbols`, da `lang.findUsagesProvider` bereits registriert ist.
- **`rename_symbol` hängt ebenfalls von `find_symbol` ab** — identischer Code-Pfad über `SymbolFinder`, `renamePsiElementProcessor` ist bereits registriert
- **Subtype-Suche ist O(n)** — iteriert über alle Klassen im Projekt. Bei Bedarf kann ein gecachter Inverse-Index (`parentName → Set<childName>`) nachgerüstet werden
- **TypeHierarchy und GDExtension-Rust-Klassen** — `GdExtensionRustResolver.getBaseClass()` nutzt Regex-Parsing der Rust-Dateien (kein PSI). Funktioniert für Standard-Fälle, kann bei ungewöhnlicher Formatierung fehlschlagen

## Nicht im Scope

- Änderungen am Serena-Projekt selbst
- Eigener GDScript Language-Server in solidlsp
- Godot Editor LSP Integration (ist unabhängig davon und funktioniert bereits)
- Cache/Inverse-Index für Subtype-Suche (Optimierung bei Bedarf)
