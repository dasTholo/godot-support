# False Positive Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate all 134 false positives in the GDScript parser plugin by fixing the `not in` compound operator and GDExtension static method detection.

**Architecture:** Two independent fixes — (1) teach `GdNegateExParser` to handle `NEGATE IN` as a compound `not in` operator, (2) extend `GdExtensionRustResolver` to detect static methods from Rust source and mark them in generated stubs.

**Tech Stack:** Kotlin, IntelliJ Platform SDK, JFlex (read-only), godot-rust convention

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `gdscript/src/main/kotlin/gdscript/parser/expr/GdNegateExParser.kt` | Modify | Handle `not in` compound operator |
| `gdscript/src/test/testData/gdscript/parser/godotTestCases/not_in_operator.gd` | Create | Test input for `not in` |
| `gdscript/src/test/testData/gdscript/parser/godotTestCases/not_in_operator.txt` | Create | Expected PSI tree for `not in` |
| `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/parser/GdGodotTest.kt` | Modify | Register `not_in_operator` test |
| `gdscript/src/main/kotlin/gdscript/extension/GdExtensionRustResolver.kt` | Modify | Detect static methods from Rust `#[func]` |
| `gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubWriter.kt` | Modify | Write `static func` for static methods |
| `gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubService.kt` | Modify | Pass static method info to stub writer |
| `gdscript/src/main/kotlin/gdscript/extension/GdExtensionTypeCollector.kt` | Modify | Add static method set to `GdExtTypeInfo` |

---

### Task 1: Fix `not in` compound operator in parser

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/parser/expr/GdNegateExParser.kt:13-20`

- [ ] **Step 1: Write the test input file**

Create `gdscript/src/test/testData/gdscript/parser/godotTestCases/not_in_operator.gd`:

```gdscript
func test():
	var arr := [1, 2, 3]
	if 4 not in arr:
		pass
	var x := 1 not in arr
	if "a" != "" and "a" not in arr:
		pass
```

- [ ] **Step 2: Register the test in `GdGodotTest.kt`**

Add to `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/parser/GdGodotTest.kt`, after line 50 (the `is_not_operator` test):

```kotlin
    @Test fun testnot_in_operator() = doTest(true, true)
```

- [ ] **Step 3: Run the test to see it fail**

Run: `cd gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.parser.GdGodotTest.testnot_in_operator" -PbuildConfiguration=Debug`

Expected: FAIL — either missing `.txt` file or parser errors in the PSI tree.

- [ ] **Step 4: Implement `not in` in `GdNegateExParser`**

Replace the `parse` method in `gdscript/src/main/kotlin/gdscript/parser/expr/GdNegateExParser.kt`:

```kotlin
package gdscript.parser.expr

import com.intellij.psi.tree.IElementType
import gdscript.parser.GdPsiBuilder
import gdscript.psi.GdTypes.IN
import gdscript.psi.GdTypes.NEGATE
import gdscript.psi.GdTypes.NEGATE_EX

// ( "!" | "not" ) logicNot | in
object GdNegateExParser : GdExprBaseParser() {

    override val EXPR_TYPE: IElementType = NEGATE_EX

    override fun parse(b: GdPsiBuilder, l: Int, optional: Boolean): Boolean {
        if (!b.recursionGuard(l, "NegateExpr")) return false
        var ok = b.consumeToken(NEGATE, pin = true)
        if (b.nextTokenIs(IN)) {
            // "not in" compound operator — consume IN and parse RHS at InExParser's level + 1
            b.advance()
            ok = ok && GdExprParser.parseFrom(b, l, optional, GdInExParser.POSITION + 1)
        } else {
            ok = ok && (GdExprParser.parseFrom(b, l, false, POSITION) || GdExprParser.parseFrom(b, l, optional, POSITION + 1))
        }
        b.errorPin(ok, "expression")

        return ok || b.pinned()
    }

}
```

Key change: After consuming `NEGATE`, check if the next token is `IN`. If yes, advance past `IN` and parse the right-hand side starting from `GdInExParser.POSITION + 1` (comparison level and below). This produces a `NEGATE_EX` node wrapping the entire `not in expr` construct.

- [ ] **Step 5: Generate the expected PSI tree**

Run the test with `doTest(true, true)`. The test framework uses the first run to show the actual PSI tree. Copy the actual PSI output from the test failure into `gdscript/src/test/testData/gdscript/parser/godotTestCases/not_in_operator.txt`.

Verify the `.txt` file contains NO `PsiErrorElement` nodes. The key patterns to check:

- `4 not in arr` → `NEGATE_EX` containing `NEGATE('not')`, `IN('in')`, and the RHS expression
- `1 not in arr` → same structure in a var assignment
- `"a" not in arr` → `NEGATE_EX` nested inside `LOGIC_AND_EX`

- [ ] **Step 6: Run the test to verify it passes**

Run: `cd gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.parser.GdGodotTest.testnot_in_operator" -PbuildConfiguration=Debug`

Expected: PASS with no errors.

- [ ] **Step 7: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/parser/expr/GdNegateExParser.kt \
        gdscript/src/test/testData/gdscript/parser/godotTestCases/not_in_operator.gd \
        gdscript/src/test/testData/gdscript/parser/godotTestCases/not_in_operator.txt \
        gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/parser/GdGodotTest.kt
git commit -m "fix(gdscript): support 'not in' compound operator in parser

The parser now recognizes 'not in' as a compound operator (e.g.,
'x not in arr'). Previously, 'not' consumed the NEGATE token and
failed to parse the following IN token, causing ~130 cascade errors.

Fixes the root cause of 134 false positives in files using 'not in'."
```

---

### Task 2: Add static method info to `GdExtTypeInfo`

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/extension/GdExtensionTypeCollector.kt:27-33`

- [ ] **Step 1: Add `staticMethods` field to `GdExtTypeInfo`**

In `gdscript/src/main/kotlin/gdscript/extension/GdExtensionTypeCollector.kt`, change the data class:

```kotlin
data class GdExtTypeInfo(
    val name: String,
    val inherits: String,
    val methods: List<GdExtMethodInfo>,
    val properties: List<GdExtPropertyInfo>,
    val signals: List<GdExtSignalInfo>,
    val staticMethods: Set<String> = emptySet()
)
```

- [ ] **Step 2: Verify compilation**

Run: `cd gdscript && ./gradlew compileKotlin -PbuildConfiguration=Debug`

Expected: PASS — the default value `emptySet()` keeps all existing call sites compatible.

- [ ] **Step 3: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/extension/GdExtensionTypeCollector.kt
git commit -m "refactor(gdext): add staticMethods field to GdExtTypeInfo"
```

---

### Task 3: Detect static methods in `GdExtensionRustResolver`

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/extension/GdExtensionRustResolver.kt`

- [ ] **Step 1: Add the static method detection pattern and collection method**

Add a new companion pattern and a public method to `GdExtensionRustResolver`:

```kotlin
companion object {
    fun getInstance(project: Project): GdExtensionRustResolver =
        project.getService(GdExtensionRustResolver::class.java)

    private val GODOT_CLASS_PATTERN = Regex(
        """#\[derive\([^)]*GodotClass[^)]*\)\]\s*(?:#\[class\(([^)]*)\)\]\s*)?(?:pub\s+)?struct\s+(\w+)""",
        RegexOption.MULTILINE
    )

    private val RENAME_PATTERN = Regex("""rename\s*=\s*(\w+)""")

    // Matches #[func] fn method_name(params) — captures method name and parameter list
    private val FUNC_PATTERN = Regex(
        """#\[func\]\s*(?:pub\s+)?fn\s+(\w+)\s*\(([^)]*)\)""",
        RegexOption.MULTILINE
    )
}

/**
 * Collect static method names for a given GDExtension class.
 * A method is static if it has #[func] and its first parameter is NOT &self or &mut self.
 * In godot-rust, this means it's callable as ClassName.method() from GDScript.
 */
fun collectStaticMethods(className: String): Set<String> {
    val mapping = buildClassNameMapping()
    val location = mapping[className] ?: return emptySet()

    val content = try {
        String(location.virtualFile.contentsToByteArray(), Charsets.UTF_8)
    } catch (_: Exception) {
        return emptySet()
    }

    val staticMethods = mutableSetOf<String>()
    FUNC_PATTERN.findAll(content).forEach { match ->
        val methodName = match.groupValues[1]
        val params = match.groupValues[2].trim()
        // Static if first param is NOT &self or &mut self
        if (!params.startsWith("&self") && !params.startsWith("&mut self")) {
            staticMethods.add(methodName)
        }
    }

    return staticMethods
}
```

Note: This scans ALL `#[func]` methods in the file, not just those within a specific `impl` block. This is acceptable because `#[func]` is only valid inside `#[godot_api] impl`, so false matches are not possible.

- [ ] **Step 2: Verify compilation**

Run: `cd gdscript && ./gradlew compileKotlin -PbuildConfiguration=Debug`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/extension/GdExtensionRustResolver.kt
git commit -m "feat(gdext): detect static methods from Rust #[func] attributes

Methods marked with #[func] whose first parameter is not &self or
&mut self are considered static (godot-rust convention)."
```

---

### Task 4: Generate `static func` in stubs

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubWriter.kt:29-56`

- [ ] **Step 1: Update `generateStub` to use `staticMethods`**

Replace the `generateStub` method in `GdExtensionStubWriter.kt`:

```kotlin
private fun generateStub(type: GdExtTypeInfo): String {
    val sb = StringBuilder()

    sb.appendLine("extends ${type.inherits}")
    sb.appendLine("class_name ${type.name}")
    sb.appendLine()

    // Signals
    for (signal in type.signals) {
        sb.appendLine("signal ${signal.name}()")
    }
    if (type.signals.isNotEmpty()) sb.appendLine()

    // Properties
    for (prop in type.properties) {
        sb.appendLine("var ${prop.name}: ${prop.type}")
    }
    if (type.properties.isNotEmpty()) sb.appendLine()

    // Methods
    for (method in type.methods) {
        val params = method.params.joinToString(", ") { "${it.first}: ${it.second}" }
        val ret = if (method.returnType == "void") " -> void" else " -> ${method.returnType}"
        val prefix = if (method.name in type.staticMethods) "static func" else "func"
        sb.appendLine("$prefix ${method.name}($params)$ret: pass")
    }

    return sb.toString()
}
```

Key change: The only difference is the `prefix` variable that checks `type.staticMethods`.

- [ ] **Step 2: Verify compilation**

Run: `cd gdscript && ./gradlew compileKotlin -PbuildConfiguration=Debug`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubWriter.kt
git commit -m "feat(gdext): write 'static func' for static GDExtension methods"
```

---

### Task 5: Wire static method detection into stub generation

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubService.kt:47-52`

- [ ] **Step 1: Pass static method info from Rust resolver to type infos**

In `GdExtensionStubService.kt`, modify the `generateStubs()` method. After line 48 (`val typeInfos = collector.collectAllTypeDetails(extensionTypes)`) and before line 52 (`GdExtensionStubWriter.writeStubs(typeInfos, stubDir)`), add the Rust resolver enrichment:

```kotlin
fun generateStubs() {
    val basePath = project.basePath ?: return
    val godotDir = findGodotProjectDir(java.io.File(basePath))
    val godotPath = godotDir?.absolutePath ?: basePath

    thisLogger().info("Starting GDExtension stub generation via existing Godot LSP connection")

    val collector = GdExtensionTypeCollector(project, godotPath)

    // Step 1: Get all type names from LSP
    val allTypeNames = collector.collectAllTypeNames()
    if (allTypeNames.isEmpty()) {
        thisLogger().warn("No types returned from Godot LSP")
        return
    }

    // Step 2: Filter to GDExtension-only types
    val knownTypes = getKnownSdkTypes()
    val extensionTypes = allTypeNames.filter { it !in knownTypes }

    if (extensionTypes.isEmpty()) {
        thisLogger().info("No GDExtension types found")
        return
    }

    thisLogger().info("Found ${extensionTypes.size} potential GDExtension types, collecting details...")

    // Step 3: Collect details for all extension types via LSP (batched)
    val typeInfos = collector.collectAllTypeDetails(extensionTypes)

    // Step 3b: Enrich with static method info from Rust source
    val rustResolver = GdExtensionRustResolver.getInstance(project)
    val enrichedTypeInfos = typeInfos.map { type ->
        val staticMethods = rustResolver.collectStaticMethods(type.name)
        if (staticMethods.isNotEmpty()) type.copy(staticMethods = staticMethods) else type
    }

    // Step 4: Write stub files
    val stubDir = getStubDirectory()
    GdExtensionStubWriter.writeStubs(enrichedTypeInfos, stubDir)

    // Step 5: Register as library
    GdLibraryManager.registerLibrary(LIBRARY_NAME, stubDir, project)

    thisLogger().info("GDExtension stub generation complete: ${enrichedTypeInfos.size} types")
}
```

Key changes:
- Import `GdExtensionRustResolver` at the top
- After `collectAllTypeDetails`, iterate types and enrich with `collectStaticMethods`
- Pass `enrichedTypeInfos` to `writeStubs` instead of `typeInfos`

- [ ] **Step 2: Verify compilation**

Run: `cd gdscript && ./gradlew compileKotlin -PbuildConfiguration=Debug`

Expected: PASS.

- [ ] **Step 3: Manual verification**

To verify end-to-end, regenerate stubs in RustRover:
1. Open P24h project
2. Run GDExtension stub generation (via action or on project open)
3. Check the generated `ZoneMapResource.gd` stub file at `~/.config/JetBrains/RustRover*/gdext-stubs/*/ZoneMapResource.gd`
4. Verify `from_keypoints` now has `static func` prefix:

```gdscript
static func from_keypoints(p0: Variant = null, ...) -> Variant: pass
func get_zone_at(p0: Variant = null, ...) -> Variant: pass
```

5. Verify `game_manager.gd` line 198 no longer shows "Reference [from_keypoints] not found"

- [ ] **Step 4: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubService.kt
git commit -m "feat(gdext): enrich stubs with static method info from Rust source

The stub generation now queries GdExtensionRustResolver to identify
static methods (no &self parameter in #[func] Rust source) and
generates 'static func' in the GDScript stubs.

Fixes false positive: Reference [from_keypoints] not found on
ZoneMapResource."
```

---

### Task 6: Run full test suite and verify

**Files:** None (verification only)

- [ ] **Step 1: Run all parser tests**

Run: `cd gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.parser.*" -PbuildConfiguration=Debug`

Expected: All existing tests still pass. The new `testnot_in_operator` test passes.

- [ ] **Step 2: Check that `is_not_operator` test still has same behavior**

The `is_not_operator` test is `@Ignore("RIDER-126458")` — our changes should NOT affect it. The `is not` pattern is different: it's `IS` + `NEGATE` (handled in `GdIsExParser`), not `NEGATE` + `IN`. Verify it's still ignored and unchanged.

- [ ] **Step 3: Final commit if any cleanup needed**

If all tests pass, no further action needed.
