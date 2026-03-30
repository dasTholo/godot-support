# False Positive Fixes Phase 3 Design

Eliminate remaining false positives caused by stub generation bugs, missing signal parameters, and a lambda-in-args parser issue.

## Fix 1: Nested Generics in Return Type Regex

### Problem

`FUNC_FULL_PATTERN` regex captures return types with only one level of angle brackets. `Array<Gd<ActionResource>>` is captured as `Array<Gd<ActionResource>` (missing closing `>`), which `mapRustType` cannot match to the `Array<Gd<T>>` pattern. Result: `get_queue_actions() -> ActionResource` instead of `-> Array[ActionResource]`.

Same bug affects `Option<Gd<T>>`, `Dictionary<K,V>`, and any nested generic.

### Solution

Replace the return-type capture group in `FUNC_FULL_PATTERN` with one that supports nested angle brackets. Since Java/Kotlin regex cannot match balanced brackets, extract the return type using a simple character scan that counts `<`/`>` depth, similar to the existing `splitRustParams()` helper.

Concretely: change the regex to capture everything after `->` up to `{` or end-of-line, then trim whitespace. The balanced-bracket parsing already exists in `mapRustType` — it just needs to receive the full type string.

### Files Changed

- `GdExtensionRustResolver.kt` — replace `FUNC_FULL_PATTERN` regex capture group 4 with a broader pattern, or switch to a two-step approach: regex captures to end of return type region, then a helper trims it.

### Expected Result

Before: `func get_queue_actions(member_id: String) -> ActionResource: pass`
After: `func get_queue_actions(member_id: String) -> Array[ActionResource]: pass`

## Fix 2: Remove Inherited Members from Stubs

### Problem

GDExtension stubs include 60+ inherited methods, 13+ inherited properties, and 13+ inherited signals from Object/RefCounted/Node. These use `Variant` types and shadow the correct SDK signatures. Examples: `signal ready()`, `var name: Variant`, `func connect(p0: Variant = null, ...) -> Variant`.

### Solution

When Rust source data is available: write **only** Rust-sourced methods and Rust-sourced signals. Do not write any LSP-collected methods, properties, or signals — the `extends Node` mechanism handles inheritance correctly.

When only LSP data is available (no Rust source, e.g. C++ GDExtensions or GDScript plugins like GdUnit4): filter out inherited members using the SDK. Specifically, collect member names from the base class's SDK stub file and exclude any method/property/signal whose name appears in the SDK base class.

### Approach for LSP-only Filtering

Load the SDK `.gd` file for the type's `inherits` class (e.g., `RefCounted.gd` for GdUnit4 types). Extract all method names, property names, and signal names. Exclude any LSP-collected member whose name appears in that set. This is dynamic (works for any base class) and doesn't require a hardcoded exclude list.

### Files Changed

- `GdExtensionStubWriter.kt` — when `rustMethods` is present, skip LSP properties/signals entirely. When `rustMethods` is null, filter inherited members.
- `GdExtensionStubService.kt` — pass SDK path info to the writer or build the inherited-member sets before writing.

### Expected Result

Before (DelegationManagerNode):
```gdscript
extends Node
class_name DelegationManagerNode
signal task_completed()
signal ready()
signal renamed()
# ... 13 more inherited signals
var name: Variant
var owner: Variant
# ... 13 more inherited properties
func initialize(...) -> void: pass
func get_class() -> Variant: pass
# ... 50+ inherited methods
```

After:
```gdscript
extends Node
class_name DelegationManagerNode
signal task_completed(member_id: String, action_id: String, action_name: String, success: bool, timestamp: float)
func initialize(team: Array[TeamMemberResource], ...) -> void: pass
func get_queue_actions(member_id: String) -> Array[ActionResource]: pass
# only Rust-sourced methods, no inherited members
```

## Fix 3: Extract Signal Parameters from Rust Source

### Problem

Signals are declared with empty parameter lists: `signal task_completed()`. The Rust source has full parameter info: `#[signal] fn task_completed(member_id: GString, action_id: GString, action_name: GString, success: bool, timestamp: f64)`.

The LSP only provides signal names (kind=23), not parameters. The current Rust resolver only extracts `#[func]` methods, not `#[signal]` declarations.

### Solution

Add a `collectSignals()` method to `GdExtensionRustResolver` that parses `#[signal] fn name(params)` declarations using a regex similar to `FUNC_FULL_PATTERN`. Map parameter types using the existing `mapRustType`. Return a list of signal info (name + params).

Add a `rustSignals` field to `GdExtTypeInfo`. The stub writer uses these when available.

### Regex Pattern

`#[signal]` functions never have return types and never have `&self`. Pattern:
```
#\[signal\]\s*fn\s+(\w+)\s*\(([^)]*)\)
```

### Files Changed

- `GdExtensionRustResolver.kt` — add `SIGNAL_PATTERN` regex and `collectSignals()` method.
- `GdExtensionTypeCollector.kt` — add `RustSignalInfo` data class, add `rustSignals` field to `GdExtTypeInfo`.
- `GdExtensionStubService.kt` — call `collectSignals()` and pass to type info.
- `GdExtensionStubWriter.kt` — write Rust-sourced signals with parameters when available.

### Expected Result

Before: `signal task_completed()`
After: `signal task_completed(member_id: String, action_id: String, action_name: String, success: bool, timestamp: float)`

## Fix 4: Lambda Followed by Comma in Argument List (Parser)

### Problem

This valid GDScript causes a parse error:
```gdscript
signal.connect(
    func(scenarios: Array) -> void:
        if scenarios.size() > 0:
            store.load_scenario(scenarios[0].get("id", "")),
    CONNECT_ONE_SHOT,
)
```

Error: `END_STMT expected / DEDENT expected` at the comma after the lambda body.

### Root Cause

The `GdStmtParser.suite()` method handles multiline lambda bodies. When the lambda body ends with a compound statement (if/for/while), the DEDENT handling logic (lines 55-63) doesn't properly account for the case where COMMA follows DEDENT (indicating the next argument in the parent call). The method checks for `DEDENT, NEW_LINE` or `DEDENT, END_STMT` but not `DEDENT, COMMA`.

Specifically: when `asLambda=true` and inside `isArgs` context, the suite's DEDENT handling remaps or consumes the DEDENT token but doesn't signal to the parent `GdArgListParser` that the lambda has cleanly ended and a COMMA should follow.

### Solution

In `GdStmtParser.suite()`, when `asLambda=true` and `b.isArgs`, add COMMA to the set of valid tokens that can follow a DEDENT to signal lambda termination. When the token after DEDENT is COMMA, consume the DEDENT and let the parent `GdArgListParser` handle the COMMA.

This is a targeted fix in the DEDENT-handling block of `suite()` (around lines 55-63 in GdStmtParser.kt).

### Files Changed

- `GdStmtParser.kt` — extend DEDENT handling in `suite()` for the `asLambda && isArgs` case.

### Test Case

The existing test `signal_connect_multiline_lambda` should pass after this fix. Add a more complex case with `if`-body inside the lambda.

## Fix 5: mapRustType Rule Ordering Bug

### Problem

`initialize(team: TeamMemberResource, ...)` should be `initialize(team: Array[TeamMemberResource], ...)`. The Rust source has `team: Array<Gd<TeamMemberResource>>` but the type mapping produces `TeamMemberResource` instead of `Array[TeamMemberResource]`.

### Root Cause

In `mapRustType()`, the `Gd<T>` rule is checked **before** `Array<Gd<T>>` and `Option<Gd<T>>`. The regex `Gd<(\w+)>` matches the inner `Gd<TeamMemberResource>` within `Array<Gd<TeamMemberResource>>`, short-circuiting the function and returning `TeamMemberResource` before the `Array<Gd<T>>` rule is ever reached.

### Solution

Reorder the rules in `mapRustType()`: check compound types (`Array<Gd<T>>`, `Option<Gd<T>>`) **before** the simple `Gd<T>` rule. This ensures the more specific patterns match first.

### Files Changed

- `GdExtensionRustResolver.kt` — reorder match rules in `mapRustType()`.

### Expected Result

Before: `func initialize(team: TeamMemberResource, role_modifiers: RoleModifierResource, ...)`
After: `func initialize(team: Array[TeamMemberResource], role_modifiers: Array[RoleModifierResource], ...)`

## Summary

| Fix | Type | False Positives Fixed |
|-----|------|----------------------|
| 1. Nested generics | Stub generation | `get_queue_actions().size()`, `[0].get_action_id()`, `patients.is_empty()` |
| 2. Remove inherited | Stub generation | `signal.connect()` not shadowed, no Variant properties |
| 3. Signal params | Stub generation | `task_completed.connect(func(a,b,c): ...)` resolves |
| 4. Lambda-in-args | Parser | Parse error on multiline lambda with trailing comma |
| 5. Array param types | Stub generation | `initialize(team: Array[TeamMemberResource], ...)` correct |

## Out of Scope

- `Engine.get_main_loop().root` — correct behavior per Godot API. Fix is cast in user code.
