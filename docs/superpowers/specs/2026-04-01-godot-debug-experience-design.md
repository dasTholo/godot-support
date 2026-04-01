# Godot Debug Experience für RustRover

**Datum:** 2026-04-01
**Branch:** `feature/godot-debug-experience` (von `253-unified`)
**Ansatz:** Bottom-Up (Value Presentation → Children/Scopes → Expression Evaluation → LLDB Printers → Scene Tree Tab)

## Zusammenfassung

Erweiterung der bestehenden GDScript-DAP-Integration um Custom Value Rendering, erweiterte Expression Evaluation, einen Scene Tree Debug-Tab und LLDB Pretty-Printers für Rust gdext. Ziel: Debug-Erlebnis auf dem Niveau des ehemaligen Rider-Debuggers, angepasst an RustRover und den DAP-Pfad.

## Ausgangslage

- DAP-Verbindung zu Godot Editor (Port 6006) existiert bereits
- Run Configurations (Launch/Attach), Line/Exception Breakpoints vorhanden
- Expression Evaluation Grundgerüst vorhanden (`GdScriptDebuggerEditorsProvider`)
- Kein Custom Value Rendering, keine Godot-spezifische Variable-Darstellung
- Godot's DAP-Server liefert `type`-String mit (z.B. `"Vector2"`, `"Color"`, `"Node2D"`)

## Architektur

### Einstiegspunkt

```
DebugAdapterDescriptor (existing, in GdScriptDebugAdapterSupportProvider)
  └─ override createXDebugProcess()
       └─ GdScriptDapDebugProcess extends DapXDebugProcess
            └─ override getPresentationFactory()
                 └─ GdScriptDapPresentationFactory extends DefaultDapXDebuggerPresentationFactory
                      ├─ override createValue() → GdScriptDapXValue
                      ├─ override createScope() → GdScriptDapScope
                      └─ override createStackFrame() → default
```

### Neues Package

`gdscript.dap.presentation` — enthält die PresentationFactory, Value-Klasse und alle Presenter.

### Type-Registry

```kotlin
GdScriptValuePresenterRegistry
  ├─ ColorPresenter       → type == "Color"
  ├─ VectorPresenter      → type matches Vector2|Vector2i|Vector3|Vector3i|Vector4|Vector4i|Rect2|Rect2i|AABB|Plane|Quaternion
  ├─ TransformPresenter   → type matches Transform2D|Transform3D|Basis
  ├─ NodePresenter        → type ist Node-Subklasse (SDK-Stub-Lookup)
  ├─ CollectionPresenter  → type == Array|Dictionary
  └─ DefaultPresenter     → Fallback (wie bisher)
```

Interface:

```kotlin
interface GdScriptValuePresenter {
    fun canPresent(type: String): Boolean
    fun createPresentation(variable: DapVariable, hasChildren: Boolean): XValuePresentation
    fun getIcon(variable: DapVariable): Icon?  // optional, null = default
}
```

## A) Value Presenters

### ColorPresenter

- **Input:** `type="Color"`, `value="(1, 0, 0, 1)"`
- **Output:** Farbiges 12x12px Icon neben `Color(1, 0, 0, 1)`
- Parst RGBA aus Value-String, generiert `ColorIcon` via `java.awt.Color`

### VectorPresenter

- **Input:** `type="Vector2"`, `value="(1.5, 3.2)"`
- **Output:** `Vector2(1.5, 3.2)` — kompakt, einzeilig
- Deckt ab: Vector2/2i/3/3i/4/4i, Rect2/2i, AABB, Plane, Quaternion

### TransformPresenter

- **Input:** `type="Transform2D"`, `value="((1, 0), (0, 1), (0, 0))"`
- **Output:** Mehrzeilig mit benannten Achsen (x, y, origin)
- Deckt ab: Transform2D, Transform3D, Basis

### NodePresenter

- **Input:** `type="CharacterBody2D"`, `value="<CharacterBody2D#1234>"`
- **Output:** `CharacterBody2D "Player"` mit Typ-Icon
- Erkennung via SDK-Stub-Lookup (alle Node-Subklassen)
- Node-Name aus erstem Child-Property `name` extrahiert
- Custom Gruppen: [Properties] + [Scene Children]

### CollectionPresenter

- **Array:** `Array (3 elements)` mit Index-basiertem Expand, Chunked Loading bei >100 Elementen
- **Dictionary:** `Dictionary (5 entries)` mit Key-Value-Darstellung
- Nutzt `DapStructuredVariable.indexedVariables`/`namedVariables`

### EnumPresenter (Low-Priority)

- Nur wenn Kontext bekannt ist (SDK-Stub-Mapping)
- `int` → Enum-Name wenn Parameter-Typ ein Enum ist
- Kann wegfallen wenn zu aufwändig

### DefaultPresenter (Fallback)

- Identisch zum aktuellen Verhalten: `XRegularValuePresentation(value, type)`
- Greift für `int`, `float`, `bool`, `String` und alles Unbekannte

## B) Custom Children & Scopes

### Node Scene Children

- Bei Node-Typen: zusätzliche expandierbare Gruppe "Scene Children"
- Implementiert in `GdScriptDapScope` via `createScope()` override
- Daten via DAP `evaluate`-Request (`get_children()`)

### Signal Connections

- Zeige verbundene Signals als Gruppe bei Node-Typen
- Via DAP `evaluate` mit `get_signal_connection_list()`

### Collection-Verbesserungen

- Dictionary: Key-Value-Paare statt verschachtelter Generic-Properties
- Array: Chunked Loading in 100er-Blöcken bei großen Arrays
- Resource: `resource_path` prominent als erstes Property

## C) Expression Evaluation

### Typ-bewusste Completion

- Neuer `GdScriptDebugCompletionContributor`
- Holt aktuelle Variables aus DAP Scope → schlägt `type` in SDK-Stubs nach
- Liefert Properties & Methoden als Completion-Items

### Ablauf

1. User tippt im Watch/Evaluate-Fenster
2. `GdScriptDebuggerEditorsProvider` liefert PSI-Kontext
3. `GdScriptDebugCompletionContributor` matcht Variable gegen SDK-Stubs
4. Completion-Items mit Typ-Info

### Limitierung

- Zuverlässig nur für erste Ebene (`player.velocity`)
- Verkettung (`player.get_node("Sprite").texture`) begrenzt durch Godot's `evaluate`-Support

## D) Scene Tree Debug-Tab

### UI

Neuer Tool Window Tab im Debug-Fenster, nur sichtbar während GDScript-Debug-Session.

```
┌─ Scene Tree ─────────────────────────┐
│ ▼ Root                               │
│   ▼ Main (Node2D)                    │
│     ▼ Player (CharacterBody2D)  ◀━━  │ ← aktiver Scope
│       ├─ Sprite (Sprite2D)           │
│       └─ Camera (Camera2D)           │
│     ▶ EnemySpawner (Node2D)          │
└──────────────────────────────────────┘
```

### Technischer Ansatz

1. Bei Breakpoint-Stop: `evaluate`-Request für `get_tree().root`
2. Rekursiv expandieren via `variables`-Requests
3. UI: `SimpleTree` als zusätzlicher Tab im `XDebugProcess`
4. Klick auf Node → `evaluate`-Request → Properties im Variables-Panel
5. Scope-Highlighting: Aktuellen Node aus Stack-Frame-Script-Pfad matchen

### Konfiguration

- Registry-Flag: `gdscript.debug.sceneTree.enabled` (default `true`)
- Max-Tiefe: 10 Ebenen (konfigurierbar)
- Lazy Loading pro Ebene

### Limitierungen

- Nur im Stopped-State aktualisiert (kein Live-Update)
- Abhängig von Godot's `evaluate`-Support für `get_tree()`
- Erster Load kann bei großen Scenes langsam sein

## E) LLDB Pretty-Printers für Rust gdext

Unabhängig vom DAP-Pfad — läuft über RustRover's nativen LLDB-Debugger.

### Ziel-Darstellung

```
player: Gd<CharacterBody2D>  →  CharacterBody2D#1234567
velocity: Vector2             →  (150.0, -200.0)
name: GString                 →  "Player"
items: Dictionary             →  {3 entries}
variant: Variant              →  "String: hello"
```

### Umsetzung

Eine Python-Datei: `gdext_lldb_formatters.py`

**Summary Providers (einzeilig):**
- `Gd<T>` → `{class}#{instance_id}`
- `GString`, `StringName` → String-Inhalt
- `Vector2/3/4`, `Color` → Tuple-Darstellung
- `NodePath` → Pfad-String
- `Variant` → `{type}: {value}`

**Synthetic Children Providers (aufklappbar):**
- `Dictionary` → Key-Value-Paare
- `Array` → Index-basierte Elemente
- `Transform2D` → x, y, origin

### Integration

```kotlin
// GdExtLldbConfigurator — postStartupActivity
// Erkennt gdext-Projekt: Cargo.toml mit "godot" dependency oder .gdextension-Datei
// Konfiguriert LLDB: command script import <plugin-path>/gdext_lldb_formatters.py
```

Formatter-Datei lebt in:
```
gdscript/src/main/resources/debugger/gdext_lldb_formatters.py
```

### Limitierungen

- Nur `godot-rust/gdext` — nicht `gdnative` (deprecated)
- Variant-Auflösung ist best-effort (interne Repr kann sich ändern)
- Braucht `debug = true` in Cargo.toml

## Reihenfolge

1. PresentationFactory + GdScriptDapXValue + Type-Registry (Fundament)
2. Value Presenters (Color, Vector, Transform, Node, Collection, Default)
3. Custom Children/Scopes (Node Children, Signal Connections, Collection-Verbesserungen)
4. Expression Evaluation (Debug Completion Contributor)
5. LLDB Pretty-Printers (unabhängig, kann parallel zu 2-4)
6. Scene Tree Debug-Tab (experimentell, zuletzt)

## Dateien (geschätzt)

### Neue Dateien

- `gdscript/dap/presentation/GdScriptDapDebugProcess.kt`
- `gdscript/dap/presentation/GdScriptDapPresentationFactory.kt`
- `gdscript/dap/presentation/GdScriptDapXValue.kt`
- `gdscript/dap/presentation/GdScriptDapScope.kt`
- `gdscript/dap/presentation/GdScriptValuePresenterRegistry.kt`
- `gdscript/dap/presentation/GdScriptValuePresenter.kt` (Interface)
- `gdscript/dap/presentation/presenters/ColorPresenter.kt`
- `gdscript/dap/presentation/presenters/VectorPresenter.kt`
- `gdscript/dap/presentation/presenters/TransformPresenter.kt`
- `gdscript/dap/presentation/presenters/NodePresenter.kt`
- `gdscript/dap/presentation/presenters/CollectionPresenter.kt`
- `gdscript/dap/presentation/presenters/DefaultPresenter.kt`
- `gdscript/dap/evaluation/GdScriptDebugCompletionContributor.kt`
- `gdscript/dap/scenetree/GdScriptSceneTreeTab.kt`
- `gdscript/dap/scenetree/GdScriptSceneTreeModel.kt`
- `gdscript/dap/lldb/GdExtLldbConfigurator.kt`
- `gdscript/src/main/resources/debugger/gdext_lldb_formatters.py`

### Geänderte Dateien

- `gdscript/dap/GdScriptDebugAdapterSupportProvider.kt` — `createXDebugProcess()` override
- `gdscript/dap/breakpoints/GdScriptDebuggerEditorsProvider.kt` — Kontext-Erweiterung
- `gdscript/src/main/resources/META-INF/plugin.xml` — neue Extension Points registrieren
