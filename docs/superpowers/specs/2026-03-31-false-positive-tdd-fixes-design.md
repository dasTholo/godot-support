# False Positive TDD Fixes — Design Spec

**Goal:** Eliminate 6 categories of false positive errors in the GDScript plugin via TDD — test first, then fix.

**Context:** The Godot editor reports no errors for these patterns. The IntelliJ plugin's annotators/resolvers produce false positives due to gaps in type resolution, especially around GDExtension stubs and method chaining.

---

## Test Infrastructure

### New Base Class: `GdFalsePositiveTestBase`

**File:** `src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdFalsePositiveTestBase.kt`

Extends `BasePlatformTestCase`. Provides:

- `addStub(name: String, content: String)` — registers a `.gd` stub as project file via `myFixture.addFileToProject("$name.gd", content)`
- `configureTest(code: String)` — sets up the test GDScript via `myFixture.configureByText("test.gd", code)`
- `assertNoErrors()` — runs `myFixture.doHighlighting()`, filters for `HighlightSeverity.ERROR`, asserts empty
- `assertNoErrorContaining(text: String)` — same but filters for errors containing specific text
- `addArrayStub()` — convenience: registers minimal `Array.gd` with `size()`, `is_empty()`, `append()`, `clear()`
- `addNodeStubs()` — convenience: registers `Node.gd`, `MainLoop.gd`, `SceneTree.gd`, `Window.gd`, `Engine.gd`

No SDK extraction needed — stubs are self-contained project files.

### File Structure

```
src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/
  GdFalsePositiveTestBase.kt
  GdArrayMemberResolutionTest.kt      # Category 1
  GdTypedArrayAssignmentTest.kt       # Category 2
  GdArrayElementTypeTest.kt           # Category 3
  GdChainedMethodResolutionTest.kt    # Category 4
  GdMainLoopResolutionTest.kt         # Category 5
```

Category 6 (parser) uses the existing parser test infrastructure.

---

## Category 1: Array Member Resolution

**Symptom:** `get_team().size()` → "Reference [size] not found"

**Affected patterns:**
- `_scenario.get_team().size()` (game_manager.gd:182)
- `scenario.get_reeval_overrides().size()` (scenario_reeval_test.gd:11)
- `_equipment.get_contents().size()` (equipment_resource_test.gd:202)
- `pt.get_reevaluations().size()` (scenario_reeval_test.gd:67)
- `patients.is_empty()` (game_manager.gd:550)

**Root cause hypothesis:** The return type from GDExtension stub methods (e.g., `-> Array[TeamMemberResource]`) is not correctly parsed or propagated through the return-type resolution chain. `GdClassMemberUtil.listDeclarations()` already normalizes `Array[X]` → `Array` (line ~146), so the normalization logic itself is likely fine — the issue is upstream in how the return type string is read from the stub's PSI.

**Test setup:**
```
Stubs: TeamMemberResource.gd (extends Resource, class_name TeamMemberResource)
       ScenarioResource.gd (extends Resource, class_name ScenarioResource, func get_team() -> Array[TeamMemberResource]: pass)
       Array.gd (class_name Array, func size() -> int: pass, func is_empty() -> bool: pass)

Test code:
  var scenario := ScenarioResource.new()
  var team := scenario.get_team()
  var s := team.size()
  var empty := team.is_empty()

Assertion: assertNoErrors()
```

**Fix area:** Likely in `PsiGdMethodDeclUtil.getReturnType()` or `PsiGdExprUtil.getReturnType()` — the return type string from stubs with typed array returns.

---

## Category 2: Typed→Untyped Array Assignment

**Symptom:** `var result: Array = scenario.get_reeval_overrides()` → "Cannot assign Array = ReevaluationOverrideResource"

**Affected patterns:**
- scenario_reeval_test.gd lines 23, 41, 57

**Root cause hypothesis:** Same upstream issue as Category 1. The return type of `get_reeval_overrides()` is read as `ReevaluationOverrideResource` instead of `Array[ReevaluationOverrideResource]` — the `Array[...]` wrapper is lost. Once Category 1 is fixed (return type correctly reads as `Array[...]`), `GdExprUtil.typeAccepts()` should handle `Array[X]` → `Array` assignment because `arrays > 1` triggers inner-type comparison, and untyped `Array` has no inner type (blank), which returns `true`.

**Test setup:**
```
Stubs: ReevaluationOverrideResource.gd, ScenarioResource.gd (func get_reeval_overrides() -> Array[ReevaluationOverrideResource]: pass)

Test code:
  var scenario := ScenarioResource.new()
  var result: Array = scenario.get_reeval_overrides()

Assertion: assertNoErrorContaining("Cannot assign")
```

**Fix area:** Likely same fix as Category 1. If not, `GdExprUtil.typeAccepts()` needs adjustment for `Array` (untyped) accepting `Array[X]` (typed).

---

## Category 3: Array Element Type Inference

**Symptom:** `queue[0].get_action_id()` → "Reference [get_action_id] not found"

**Affected patterns:**
- delegation_manager_test.gd lines 241, 242

**Root cause hypothesis:** When indexing a typed array `Array[ActionResource]` with `[0]`, the element type `ActionResource` must be extracted via `parseFromSquare()`. If the array's type is correctly resolved (after Category 1 fix), the index expression's return type should be `ActionResource`, and `get_action_id()` should resolve against it.

**Test setup:**
```
Stubs: ActionResource.gd (func get_action_id() -> String: pass)
       DelegationManager.gd (func get_queue_actions(member: StringName) -> Array[ActionResource]: pass)

Test code:
  var mgr := DelegationManager.new()
  var queue := mgr.get_queue_actions(&"san_b")
  var id := queue[0].get_action_id()

Assertion: assertNoErrors()
```

**Fix area:** If Category 1 fix propagates correctly, this may resolve automatically. Otherwise, check `PsiGdExprUtil.getReturnType()` for `GdArrEx` (array index expression) — it should extract the element type from `Array[X]`.

---

## Category 4: GdUnit4 Chained Method Resolution

**Symptom:** `assert_array(completed_ids).contains(["rr_messen"])` → "Too many arguments"

**Affected patterns:**
- delegation_manager_test.gd lines 136, 174, 198, 323

**Root cause hypothesis:** `assert_array()` returns `GdUnitArrayAssert`. The plugin should resolve `.contains()` against `GdUnitArrayAssert`, which declares `func contains(expected: Array) -> GdUnitArrayAssert`. If the return type of `assert_array()` is not correctly resolved (e.g., returns `Variant` or empty), the plugin may fall back to `Array.contains()` which has a different signature, or fail to find the method entirely.

**Test setup:**
```
Stubs: GdUnitArrayAssert.gd (class_name GdUnitArrayAssert, func contains(expected: Array) -> GdUnitArrayAssert: pass)
       GdUnitTestSuite.gd (class_name GdUnitTestSuite, func assert_array(current: Variant) -> GdUnitArrayAssert: pass)

Test code:
  extends GdUnitTestSuite
  func test_example() -> void:
      var ids := ["a", "b"]
      assert_array(ids).contains(["a"])

Assertion: assertNoErrorContaining("Too many arguments")
```

**Fix area:** Investigate why the return type of `assert_array()` doesn't correctly resolve to `GdUnitArrayAssert`. May be related to how inherited method return types are read, or how `Variant` parameter methods interact with type inference.

---

## Category 5: `get_main_loop()` → SceneTree

**Symptom:** `Engine.get_main_loop().root` → "Reference [root] not found" + "Reference [get_node] not found"

**Affected patterns:**
- game_flow_test.gd line 22

**Root cause:** `Engine.get_main_loop()` returns `MainLoop` per SDK declaration. `root` is a property on `SceneTree` (extends `MainLoop`). The plugin can't know the runtime type is `SceneTree`.

**Fix:** Add a special-case in `PsiGdExprUtil.getReturnType()` for `get_main_loop` → `SceneTree`, following the existing pattern for `get_node` → `Node`, `get_parent` → `Node`, etc. (lines 101-136).

**Test setup:**
```
Stubs: Engine.gd (class_name Engine, static func get_main_loop() -> MainLoop: pass)
       MainLoop.gd (class_name MainLoop)
       SceneTree.gd (extends MainLoop, class_name SceneTree, var root: Window)
       Window.gd (extends Node, class_name Window, func get_node(path: NodePath) -> Node: pass)
       Node.gd (class_name Node)

Test code:
  var tree := Engine.get_main_loop()
  var r := tree.root
  var n := tree.root.get_node("SomeNode")

Assertion: assertNoErrors()
```

**Fix location:** `PsiGdExprUtil.kt` lines ~101-136, add:
```kotlin
} else if (method == "get_main_loop") {
    return "SceneTree"
}
```

---

## Category 6: Parser — DEDENT + COMMA After Lambda

**Symptom:** `store.load_scenario(scenarios[0].get("id", "")),` → "END_STMT expected / DEDENT expected"

**Affected patterns:**
- game_manager.gd line 51

**Status:** Fix exists in commit `18f18207` but is not in the current build.

**Test:** Existing parser test `signal_connect_multiline_lambda.gd` in `testData/gdscript/parser/godotTestCases/`.

**Approach:**
1. Verify the parser test passes with current code
2. If it fails, verify the fix from commit `18f18207` is correctly applied in `GdStmtParser.kt`
3. Ensure the `.txt` gold file matches the expected PSI tree

**Fix location:** `GdStmtParser.suite()` lines 56-63 — DEDENT + COMMA handling when `asLambda=true` and `b.isArgs`.

---

## Execution Order

Categories 1, 2, 3 likely share a root cause (return type from typed array stubs). Recommended order:

1. **Category 1** first — fixes the foundation (Array member resolution)
2. **Category 2** — likely passes automatically after Cat. 1 fix
3. **Category 3** — depends on Cat. 1 for correct array type propagation
4. **Category 5** — independent, simple special-case addition
5. **Category 4** — independent, chained method return type resolution
6. **Category 6** — parser fix, independent from all above

## Success Criteria

- All 6 test classes pass (green)
- Full existing test suite still passes (no regressions)
- The 17 specific false positives from the user's list are eliminated
- No new false positives introduced
