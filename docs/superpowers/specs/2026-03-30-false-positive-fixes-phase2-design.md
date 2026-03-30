# False Positive Fixes Phase 2 Design

Eliminate remaining false positives by enriching GDExtension stubs with real types from Rust source, adding varargs support, and fixing inner-enum resolution.

## Fix 1: Typed GDExtension Stubs from Rust Source

### Problem

All methods in generated GDExtension stubs use `Variant` types and generic parameter names (`p0`, `p1`...). This causes false "Reference not found" errors when accessing members on return values (e.g., `get_queue_actions()[0].get_action_id()`). Additionally, 50+ inherited methods from Object/RefCounted are duplicated in each stub with `Variant` types, shadowing the correct SDK signatures.

### Solution

`GdExtensionRustResolver` extracts complete method signatures from `#[func]` attributes in Rust source files. Type mapping converts Rust godot types to GDScript equivalents. Inherited methods are omitted — the `extends` mechanism in the SDK stubs handles inheritance correctly (verified: `GdClassMemberUtil.collectFromParents()` walks the inheritance chain).

### Type Mapping

| Rust | GDScript |
|------|----------|
| `GString`, `StringName` | `String` |
| `bool` | `bool` |
| `f32`, `f64` | `float` |
| `i32`, `i64`, `u32`, `u64` | `int` |
| `Gd<T>` | `T` |
| `Gd<Self>` | own class_name |
| `Option<Gd<T>>` | `T` |
| `Array<Gd<T>>` | `Array[T]` |
| `Array<Variant>`, `Array<VarDictionary>` | `Array` |
| `PackedStringArray`, `PackedVector2Array` etc. | direct |
| `Vector2`, `Vector2i`, `Color` etc. | direct |
| `Dictionary<K,V>`, `VarDictionary` | `Dictionary` |
| `Variant` | `Variant` |
| no return type (void fn) | `void` |

### Extends Type

Extracted from `#[class(base=Node)]` in the Rust struct annotation. Fallback: `RefCounted`.

### Files Changed

- `GdExtensionRustResolver.kt` — new method `collectMethodSignatures(className): List<RustMethodInfo>` extracting name, parameters (name + type), return type, static flag. New method `getBaseClass(className): String` extracting base from `#[class(base=...)]`.
- `GdExtensionTypeCollector.kt` — `GdExtMethodInfo` updated with real type fields. `GdExtTypeInfo` updated with base class field.
- `GdExtensionStubWriter.kt` — generates only Rust-sourced methods with real types. No more inherited methods.
- `GdExtensionStubService.kt` — prefers Rust data when available. Falls back to LSP-based generation for classes without Rust source (e.g., C++ GDExtensions).

### Expected Result

Before:
```gdscript
extends RefCounted
class_name DelegationManagerNode
func get_queue_actions(p0: Variant = null, ...) -> Variant: pass
func initialize(p0: Variant = null, ...) -> Variant: pass
func get_class() -> Variant: pass
# ... 50+ inherited Object methods with Variant ...
```

After:
```gdscript
extends Node
class_name DelegationManagerNode
func get_queue_actions(member_id: String) -> Array[ActionResource]: pass
func initialize(team: Array[TeamMemberResource], role_modifiers: Array[RoleModifierResource], difficulty: DifficultyResource, situation_modifiers: Array[SituationModifierResource], player_member_id: String) -> void: pass
```

## Fix 2: Varargs Support (`...` Syntax)

### Problem

GDScript supports varargs via `...` prefix syntax (e.g., `func contains(...expected: Array)`). The plugin parser only recognizes the `vararg` keyword as a vararg marker, not the `...` prefix. This causes false "Too many arguments" errors on any call to vararg methods (affects GdUnit4 and any addon using varargs).

### Solution

Extend the lexer and parameter parser to recognize `...identifier` as a vararg parameter.

### Files Changed

- `Gd.flex` — add `...` as a new token (e.g., `ELLIPSIS`). Currently only `..` exists as `DOTDOT`.
- `GdLexer.java` (generated) — regenerated from flex.
- Parameter parser — recognize `ELLIPSIS` before identifier in parameter declarations, setting the vararg flag so `isVariadic` returns true.
- `GdTypes` / PSI — may need a new `ELLIPSIS` token type.

### Scope

The `...` syntax is official GDScript since Godot 4.x. The existing `GdParamAnnotator` already has `if (declaration.isVariadic) return` — only the parsing/detection is missing.

## Fix 3: Inner Enum Resolution (`Class.Enum.VALUE`)

### Problem

`EquipmentPanel.SlotPosition.NORTH` reports "Reference [NORTH] not found". The resolver handles inner-**classes** for multi-level dot access but not inner-**enums**.

### Solution

Extend the member resolution to treat inner enum declarations like inner classes during dot-access resolution.

### Files Changed

- `GdClassMemberUtil.kt` (around line 165-175) — when listing class members, include enum declarations as valid results (not just enum values). This allows `EquipmentPanel.SlotPosition` to resolve to the enum declaration.
- `GdClassMemberReference.kt` (around line 174-181) — allow inner enums to pass through the static/instance access filter, same as inner classes.
- `GdRefIdAnnotator.kt` (around line 90-98) — ensure the annotator does not mark enum members as unresolved when accessed via `Class.Enum.VALUE`.

### Scope

Pure resolver logic change. No parser or lexer modifications needed.

## Summary

| Fix | Type | False Positives Fixed |
|-----|------|----------------------|
| 1. Typed stubs | Stub generation | `get_action_id` not found (2) + any method/type on GDExtension objects |
| 2. Varargs | Lexer + Parser | "Too many arguments" on vararg methods (6+) |
| 3. Inner enums | Resolver | `Class.Enum.VALUE` not found (4+) |

## Out of Scope

- `Engine.get_main_loop().root` — correct behavior, `get_main_loop()` returns `MainLoop` per Godot API. Fix is a cast in user code: `(Engine.get_main_loop() as SceneTree).root`.
- `REDUNDANT_AWAIT` — GdUnit4-specific, low priority.
- SDK stubs for Godot 4.5/4.6 — tracked in Plugin-README.md TODO. Upstream issue.
