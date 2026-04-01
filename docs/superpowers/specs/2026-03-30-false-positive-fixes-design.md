# False Positive Fixes Design

Fixing 134 false positives reported by the GDScript parser plugin that the Godot editor correctly handles as valid code.

## Problem

A checkup comparing the plugin's GDScript parser against the Godot LSP revealed three categories of false positives in `game_manager.gd`:

| Category | Count | Root Cause |
|----------|-------|------------|
| `not in` parser bug | 3 | Parser doesn't recognize `not in` as a compound operator |
| GDExtension reference not found | 1 | Static methods not marked `static` in generated stubs |
| Cascade errors (lines 386-423) | ~130 | Parser loses sync after the `not in` failure |

All 134 errors are false positives. The Godot editor reports zero errors.

## Fix 1: `not in` Compound Operator

### Current Behavior

The expression `action_id not in _active_continuous_actions` (line 385) fails because:

1. Lexer tokenizes `not` as `NEGATE` and `in` as `IN` (correct)
2. `GdNegateExParser` consumes `NEGATE`, pins, then tries to parse a sub-expression
3. `IN` is a *nested* operator (not an expression start) so expression parsing fails
4. Parser crashes, producing ~130 cascade errors through the rest of the function

### Solution: Parser-Level Compound (Approach B)

Modify `GdNegateExParser.parse()` to detect `NEGATE` followed by `IN` and handle it as a compound `not in` operator.

**File:** `gdscript/src/main/kotlin/gdscript/parser/expr/GdNegateExParser.kt`

```kotlin
override fun parse(b: GdPsiBuilder, l: Int, optional: Boolean): Boolean {
    if (!b.recursionGuard(l, "NegateExpr")) return false
    var ok = b.consumeToken(NEGATE, pin = true)
    if (b.nextTokenIs(IN)) {
        // "not in" compound operator: consume IN, parse RHS
        b.advance() // consume IN
        ok = ok && GdExprParser.parseFrom(b, l, optional, GdInExParser.POSITION + 1)
    } else {
        ok = ok && (GdExprParser.parseFrom(b, l, false, POSITION)
                    || GdExprParser.parseFrom(b, l, optional, POSITION + 1))
    }
    b.errorPin(ok, "expression")
    return ok || b.pinned()
}
```

**PSI tree:** Produces a `NEGATE_EX` node containing the `in` expression. No new PSI element type needed.

**Why not a lexer-level compound token (Approach C):** `not` and `in` are separated by whitespace. A flex pattern like `"not"[ \t]+"in"` is fragile with multiple spaces, comments, or newlines between the tokens. The parser level handles this robustly since the lexer has already normalized whitespace.

### Test

New test file `not_in_operator.gd` (analogous to existing `is_not_operator.gd`):

```gdscript
func test():
    var arr := [1, 2, 3]
    if 4 not in arr:
        pass
    var x := 1 not in arr
    if "a" != "" and "a" not in arr:
        pass
```

With corresponding `.txt` expected PSI tree output.

## Fix 2: GDExtension Static Methods

### Current Behavior

`ZoneMapResource.from_keypoints(keypoints, width, height)` (line 198) reports "Reference [from_keypoints] not found" because the generated stub declares it as an instance method:

```gdscript
func from_keypoints(p0: Variant = null, ...) -> Variant: pass
```

But it's called as a static method on the class itself.

### Solution: Rust Source Analysis (Approach C)

Extend `GdExtensionRustResolver` to scan `#[func]` attributes in Rust source and detect static methods via the `gd_self` parameter convention used by godot-rust.

**File:** `gdscript/src/main/kotlin/gdscript/extension/GdExtensionRustResolver.kt`

New method:

```kotlin
fun collectStaticMethods(className: String): Set<String>
```

This scans Rust files for `#[func]` methods within the `impl` block for the given class. A method is static if its first parameter is `gd_self` (godot-rust convention).

**Regex pattern for `#[func]` methods:**

```
#\[func\]\s*(pub\s+)?fn\s+(\w+)\s*\(([^)]*)\)
```

If the first parameter contains `gd_self` -> static method.

**File:** `gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubWriter.kt`

`generateStub()` receives a `Set<String>` of static method names and writes `static func` instead of `func` for those methods.

**File:** `gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubService.kt`

After collecting type details, query `GdExtensionRustResolver` for static methods per type and pass the info to the stub writer.

### Expected Result

Generated stub for `ZoneMapResource`:

```gdscript
static func from_keypoints(p0: Variant = null, ...) -> Variant: pass
func get_zone_at(p0: Variant = null, ...) -> Variant: pass
```

**Fallback:** Methods without a matching Rust source remain as instance methods (existing behavior).

## Fix 3: Cascade Errors (Lines 386-423)

No standalone fix needed. The ~130 cascade errors are a direct consequence of the `not in` parser failure in Fix 1. Once `not in` is parsed correctly, the parser stays in sync and these errors disappear.

## Summary

| Fix | Approach | Files Changed | False Positives Eliminated |
|-----|----------|---------------|---------------------------|
| 1. `not in` | Parser compound in `GdNegateExParser` | `GdNegateExParser.kt` + test | ~133 |
| 2. GDExtension static | Rust source `gd_self` detection | `GdExtensionRustResolver.kt`, `GdExtensionStubWriter.kt`, `GdExtensionStubService.kt` | 1 |
| 3. Cascade errors | Automatic via Fix 1 | none | ~130 (subset of Fix 1) |

Total: all 134 false positives eliminated.
