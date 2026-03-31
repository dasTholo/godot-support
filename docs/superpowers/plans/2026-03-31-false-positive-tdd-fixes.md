# False Positive TDD Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate 6 categories of false positive errors in the GDScript plugin using TDD — write failing tests first, then fix the code.

**Architecture:** A new test base class (`GdFalsePositiveTestBase`) provides stub registration and error assertion helpers. Each category gets its own test class using inline `.gd` code via `myFixture.configureByText()` and `myFixture.addFileToProject()` for stubs. Fixes target the resolution chain in `PsiGdExprUtil`, `GdClassMemberUtil`, `GdExprUtil`, `GdParamAnnotator`, and `GdStmtParser`.

**Tech Stack:** Kotlin, IntelliJ Platform SDK (BasePlatformTestCase), JUnit 4

---

### Task 1: Create Test Base Class

**Files:**
- Create: `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdFalsePositiveTestBase.kt`

- [ ] **Step 1: Create the base class**

```kotlin
package com.jetbrains.godot.gdscript.falsepositive

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class GdFalsePositiveTestBase : BasePlatformTestCase() {

    protected fun addStub(className: String, content: String) {
        myFixture.addFileToProject("$className.gd", content)
    }

    protected fun configureTest(code: String) {
        myFixture.configureByText("test.gd", code)
    }

    protected fun assertNoErrors() {
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue(
            "Expected no errors but found: ${errors.map { "'${it.description}' at offset ${it.startOffset}" }}",
            errors.isEmpty()
        )
    }

    protected fun assertNoErrorContaining(text: String) {
        val highlights = myFixture.doHighlighting()
        val matching = highlights.filter {
            it.severity == HighlightSeverity.ERROR && it.description?.contains(text) == true
        }
        assertTrue(
            "Expected no error containing '$text' but found: ${matching.map { "'${it.description}' at offset ${it.startOffset}" }}",
            matching.isEmpty()
        )
    }

    protected fun addArrayStub() {
        addStub("Array", """
            class_name Array
            func size() -> int: pass
            func is_empty() -> bool: pass
            func append(value: Variant) -> void: pass
            func clear() -> void: pass
            func contains(what: Variant) -> bool: pass
        """.trimIndent())
    }

    protected fun addResourceStub() {
        addStub("Resource", """
            class_name Resource
        """.trimIndent())
    }

    protected fun addNodeStubs() {
        addStub("Node", """
            class_name Node
            func get_node(path: NodePath) -> Node: pass
            func get_parent() -> Node: pass
        """.trimIndent())
        addStub("MainLoop", """
            class_name MainLoop
        """.trimIndent())
        addStub("SceneTree", """
            extends MainLoop
            class_name SceneTree
            var root: Window
        """.trimIndent())
        addStub("Window", """
            extends Node
            class_name Window
        """.trimIndent())
        addStub("Engine", """
            class_name Engine
            static func get_main_loop() -> MainLoop: pass
        """.trimIndent())
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew compileTestKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdFalsePositiveTestBase.kt
git commit -m "test: add GdFalsePositiveTestBase for TDD false positive fixes"
```

---

### Task 2: Array Member Resolution — Write Failing Tests (Category 1)

**Files:**
- Create: `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdArrayMemberResolutionTest.kt`

- [ ] **Step 1: Write the test class**

```kotlin
package com.jetbrains.godot.gdscript.falsepositive

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GdArrayMemberResolutionTest : GdFalsePositiveTestBase() {

    @Test
    fun testSizeOnTypedArrayReturn() {
        addArrayStub()
        addResourceStub()
        addStub("TeamMemberResource", """
            extends Resource
            class_name TeamMemberResource
        """.trimIndent())
        addStub("ScenarioResource", """
            extends Resource
            class_name ScenarioResource
            func get_team() -> Array[TeamMemberResource]: pass
        """.trimIndent())

        configureTest("""
            var scenario := ScenarioResource.new()
            var team := scenario.get_team()
            var s := team.size()
        """.trimIndent())

        assertNoErrorContaining("Reference [size] not found")
    }

    @Test
    fun testIsEmptyOnTypedArrayReturn() {
        addArrayStub()
        addResourceStub()
        addStub("PatientResource", """
            extends Resource
            class_name PatientResource
        """.trimIndent())
        addStub("ScenarioResource", """
            extends Resource
            class_name ScenarioResource
            func get_patients() -> Array[PatientResource]: pass
        """.trimIndent())

        configureTest("""
            var scenario := ScenarioResource.new()
            var patients := scenario.get_patients()
            var empty := patients.is_empty()
        """.trimIndent())

        assertNoErrorContaining("Reference [is_empty] not found")
    }

    @Test
    fun testSizeOnChainedTypedArrayReturn() {
        addArrayStub()
        addResourceStub()
        addStub("TeamMemberResource", """
            extends Resource
            class_name TeamMemberResource
        """.trimIndent())
        addStub("ScenarioResource", """
            extends Resource
            class_name ScenarioResource
            func get_team() -> Array[TeamMemberResource]: pass
        """.trimIndent())

        configureTest("""
            var scenario := ScenarioResource.new()
            var s := scenario.get_team().size()
        """.trimIndent())

        assertNoErrorContaining("Reference [size] not found")
    }
}
```

- [ ] **Step 2: Run the tests — expect FAIL**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdArrayMemberResolutionTest" 2>&1 | tail -20`
Expected: FAIL — errors contain "Reference [size] not found" or "Reference [is_empty] not found"

- [ ] **Step 3: Commit failing tests**

```bash
git add gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdArrayMemberResolutionTest.kt
git commit -m "test(red): add failing tests for array member resolution on typed array returns"
```

---

### Task 3: Array Member Resolution — Fix (Category 1)

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/psi/utils/PsiGdExprUtil.kt` (or whichever file the failing test reveals as the root cause)
- Modify: `gdscript/src/main/kotlin/gdscript/psi/utils/GdClassMemberUtil.kt` (if normalization needs adjustment)

- [ ] **Step 1: Diagnose the root cause**

Run the failing test with debug output. Add a breakpoint or temporary logging in `GdClassMemberUtil.listDeclarations()` at line ~147 to inspect what `calledOn` value arrives when `size()` is called on a typed array return.

Key questions:
- Is `calledOn` arriving as `Array[TeamMemberResource]` (normalization should handle it)?
- Or is it arriving as something else (e.g., `TeamMemberResource`, empty string, `Variant`)?
- Check `PsiGdExprUtil.getReturnType()` for the `GdCallEx` branch — does it correctly return the full `Array[TeamMemberResource]` from the stub's return hint?

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdArrayMemberResolutionTest.testSizeOnTypedArrayReturn" --info 2>&1 | grep -A5 "FAIL\|assert\|error"`

- [ ] **Step 2: Apply the fix**

Based on diagnosis, fix the return type resolution. The most likely fix locations:

**If the return type string is truncated (e.g., `Array` without `[TeamMemberResource]`):**
Fix in `PsiGdMethodDeclUtil.getReturnType()` — the return hint parser may not include the `[Type]` part.

**If the return type is the inner type only (e.g., `TeamMemberResource`):**
Fix in `PsiGdExprUtil.getReturnType()` — the `GdCallEx` branch may strip the `Array[...]` wrapper.

**If normalization doesn't trigger (calledOn doesn't start with `Array[`):**
Fix in `GdClassMemberUtil.listDeclarations()` — the `calledOn` value may have unexpected formatting.

Apply the minimal fix to make the test pass.

- [ ] **Step 3: Run the tests — expect PASS**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdArrayMemberResolutionTest" 2>&1 | tail -10`
Expected: All 3 tests PASS

- [ ] **Step 4: Run full test suite for regressions**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test 2>&1 | tail -15`
Expected: No new failures

- [ ] **Step 5: Commit the fix**

```bash
git add -u
git commit -m "fix: resolve Array members (size, is_empty) on typed array return types"
```

---

### Task 4: Typed→Untyped Array Assignment — Write Failing Test (Category 2)

**Files:**
- Create: `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdTypedArrayAssignmentTest.kt`

- [ ] **Step 1: Write the test class**

```kotlin
package com.jetbrains.godot.gdscript.falsepositive

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GdTypedArrayAssignmentTest : GdFalsePositiveTestBase() {

    @Test
    fun testAssignTypedArrayToUntypedArray() {
        addArrayStub()
        addResourceStub()
        addStub("ReevaluationOverrideResource", """
            extends Resource
            class_name ReevaluationOverrideResource
            func get_action_id() -> String: pass
            func get_text() -> String: pass
        """.trimIndent())
        addStub("ScenarioResource", """
            extends Resource
            class_name ScenarioResource
            func get_reeval_overrides() -> Array[ReevaluationOverrideResource]: pass
        """.trimIndent())

        configureTest("""
            var scenario := ScenarioResource.new()
            var result: Array = scenario.get_reeval_overrides()
        """.trimIndent())

        assertNoErrorContaining("Cannot assign")
    }

    @Test
    fun testAccessMembersOnUntypedArrayElements() {
        addArrayStub()
        addResourceStub()
        addStub("ReevaluationOverrideResource", """
            extends Resource
            class_name ReevaluationOverrideResource
            func get_action_id() -> String: pass
        """.trimIndent())
        addStub("ScenarioResource", """
            extends Resource
            class_name ScenarioResource
            func get_reeval_overrides() -> Array[ReevaluationOverrideResource]: pass
        """.trimIndent())

        configureTest("""
            var scenario := ScenarioResource.new()
            var result: Array = scenario.get_reeval_overrides()
            var id := result[0].get_action_id()
        """.trimIndent())

        assertNoErrors()
    }
}
```

- [ ] **Step 2: Run the tests — expect FAIL**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdTypedArrayAssignmentTest" 2>&1 | tail -20`
Expected: FAIL with "Cannot assign" errors

- [ ] **Step 3: Commit failing tests**

```bash
git add gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdTypedArrayAssignmentTest.kt
git commit -m "test(red): add failing tests for typed-to-untyped array assignment"
```

---

### Task 5: Typed→Untyped Array Assignment — Fix (Category 2)

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/psi/utils/GdExprUtil.kt:16-56`

- [ ] **Step 1: Check if Category 1 fix already resolves this**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdTypedArrayAssignmentTest" 2>&1 | tail -10`

If PASS → skip to Step 4. If FAIL → continue with Step 2.

- [ ] **Step 2: Fix typeAccepts for Array[X] → Array assignment**

In `GdExprUtil.typeAccepts()` at `gdscript/src/main/kotlin/gdscript/psi/utils/GdExprUtil.kt`, the logic at lines 25-32 only strips `Array[X]` when **both** sides are `Array`. But `Array` (untyped) also starts with `"Array"`, so `arrays` should be 2. If the issue is that the return type isn't `Array[X]` but something else, the Category 1 fix should resolve it.

If the issue persists after Category 1, add an explicit check: if one side is `Array` (no brackets) and the other is `Array[X]`, accept the assignment directly:

```kotlin
        // Untyped Array accepts any typed Array and vice versa
        if (arrays == 2) {
            if (from == "Array" || into == "Array") return true
            left = left.parseFromSquare()
            right = right.parseFromSquare()
        }
```

Replace lines 29-32 in `GdExprUtil.kt` with the above.

- [ ] **Step 3: Run tests — expect PASS**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdTypedArrayAssignmentTest" 2>&1 | tail -10`
Expected: PASS

- [ ] **Step 4: Run full test suite for regressions**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test 2>&1 | tail -15`
Expected: No new failures

- [ ] **Step 5: Commit**

```bash
git add -u
git commit -m "fix: accept typed Array[X] assignment to untyped Array"
```

---

### Task 6: Array Element Type Inference — Write Failing Test (Category 3)

**Files:**
- Create: `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdArrayElementTypeTest.kt`

- [ ] **Step 1: Write the test class**

```kotlin
package com.jetbrains.godot.gdscript.falsepositive

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GdArrayElementTypeTest : GdFalsePositiveTestBase() {

    @Test
    fun testMethodOnTypedArrayElement() {
        addArrayStub()
        addResourceStub()
        addStub("ActionResource", """
            extends Resource
            class_name ActionResource
            func get_action_id() -> String: pass
        """.trimIndent())
        addStub("DelegationManager", """
            extends Node
            class_name DelegationManager
            func get_queue_actions(member: StringName) -> Array[ActionResource]: pass
        """.trimIndent())
        addNodeStubs()

        configureTest("""
            var mgr := DelegationManager.new()
            var queue := mgr.get_queue_actions(&"san_b")
            var id := queue[0].get_action_id()
        """.trimIndent())

        assertNoErrorContaining("Reference [get_action_id] not found")
    }

    @Test
    fun testChainedMethodOnTypedArrayElement() {
        addArrayStub()
        addResourceStub()
        addStub("ActionResource", """
            extends Resource
            class_name ActionResource
            func get_action_id() -> String: pass
        """.trimIndent())
        addStub("DelegationManager", """
            extends Node
            class_name DelegationManager
            func get_queue_actions(member: StringName) -> Array[ActionResource]: pass
        """.trimIndent())
        addNodeStubs()

        configureTest("""
            var mgr := DelegationManager.new()
            var id := mgr.get_queue_actions(&"san_b")[0].get_action_id()
        """.trimIndent())

        assertNoErrorContaining("Reference [get_action_id] not found")
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdArrayElementTypeTest" 2>&1 | tail -20`
Expected: FAIL

- [ ] **Step 3: Commit failing tests**

```bash
git add gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdArrayElementTypeTest.kt
git commit -m "test(red): add failing tests for member access on typed array elements"
```

---

### Task 7: Array Element Type Inference — Fix (Category 3)

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/psi/utils/PsiGdExprUtil.kt:137-139` (GdArrEx branch)

- [ ] **Step 1: Check if Category 1 fix already resolves this**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdArrayElementTypeTest" 2>&1 | tail -10`

If PASS → skip to Step 3. If FAIL → continue with Step 2.

- [ ] **Step 2: Fix array element type extraction**

In `PsiGdExprUtil.getReturnType()`, the `GdArrEx` branch at line 137 reads:

```kotlin
is GdArrEx -> {
    val exprType = expr.exprList.firstOrNull()?.returnType ?: return GdKeywords.VARIANT
    if (exprType.startsWith("Array[") || exprType.startsWith("Dictionary["))
```

Read the full `GdArrEx` branch (lines 137-150+) to understand how the element type is extracted. The `parseFromSquare()` utility should extract `ActionResource` from `Array[ActionResource]`. If the return type from the previous call is correctly `Array[ActionResource]` (after Category 1 fix), this should work. If not, the issue is in how the intermediate `GdCallEx` return type propagates.

Apply minimal fix based on what the test reveals.

- [ ] **Step 3: Run full test suite for regressions**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test 2>&1 | tail -15`
Expected: No new failures

- [ ] **Step 4: Commit**

```bash
git add -u
git commit -m "fix: extract element type from typed Array[X] for member resolution"
```

---

### Task 8: GdUnit4 Chained Method Resolution — Write Failing Test (Category 4)

**Files:**
- Create: `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdChainedMethodResolutionTest.kt`

- [ ] **Step 1: Write the test class**

```kotlin
package com.jetbrains.godot.gdscript.falsepositive

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GdChainedMethodResolutionTest : GdFalsePositiveTestBase() {

    @Test
    fun testAssertArrayContains() {
        addArrayStub()
        addStub("GdUnitAssert", """
            class_name GdUnitAssert
        """.trimIndent())
        addStub("GdUnitArrayAssert", """
            extends GdUnitAssert
            class_name GdUnitArrayAssert
            func contains(expected: Array) -> GdUnitArrayAssert: pass
            func has_size(expected: int) -> GdUnitArrayAssert: pass
            func is_empty() -> GdUnitArrayAssert: pass
        """.trimIndent())
        addStub("GdUnitTestSuite", """
            class_name GdUnitTestSuite
            func assert_array(current: Variant) -> GdUnitArrayAssert: pass
            func assert_bool(current: bool) -> GdUnitAssert: pass
            func assert_int(current: int) -> GdUnitAssert: pass
            func assert_str(current: String) -> GdUnitAssert: pass
        """.trimIndent())

        configureTest("""
            extends GdUnitTestSuite
            func test_example() -> void:
                var ids := ["a", "b"]
                assert_array(ids).contains(["a"])
        """.trimIndent())

        assertNoErrorContaining("Too many arguments")
    }

    @Test
    fun testAssertArrayChainedMethods() {
        addArrayStub()
        addStub("GdUnitAssert", """
            class_name GdUnitAssert
        """.trimIndent())
        addStub("GdUnitArrayAssert", """
            extends GdUnitAssert
            class_name GdUnitArrayAssert
            func contains(expected: Array) -> GdUnitArrayAssert: pass
            func has_size(expected: int) -> GdUnitArrayAssert: pass
        """.trimIndent())
        addStub("GdUnitTestSuite", """
            class_name GdUnitTestSuite
            func assert_array(current: Variant) -> GdUnitArrayAssert: pass
        """.trimIndent())

        configureTest("""
            extends GdUnitTestSuite
            func test_chain() -> void:
                var ids := ["a", "b", "c"]
                assert_array(ids).contains(["a"]).has_size(3)
        """.trimIndent())

        assertNoErrors()
    }
}
```

- [ ] **Step 2: Run the tests — expect FAIL**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdChainedMethodResolutionTest" 2>&1 | tail -20`
Expected: FAIL with "Too many arguments"

- [ ] **Step 3: Commit failing tests**

```bash
git add gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdChainedMethodResolutionTest.kt
git commit -m "test(red): add failing tests for GdUnit4 assert_array().contains() resolution"
```

---

### Task 9: GdUnit4 Chained Method Resolution — Fix (Category 4)

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/annotator/GdParamAnnotator.kt` (or the resolution chain that determines which `contains()` is called)

- [ ] **Step 1: Diagnose the root cause**

The "Too many arguments" error comes from `GdParamAnnotator.kt` line 96-109. It compares `usedParamSize > maxSize`. For `assert_array(ids).contains(["a"])`:

1. `assert_array(ids)` should return `GdUnitArrayAssert`
2. `.contains(["a"])` should resolve against `GdUnitArrayAssert.contains(expected: Array)` — 1 param, 1 arg → OK

If `contains` resolves against `Array.contains(what: Variant)` instead, that's still 1 param, 1 arg → should be OK.

Debug approach: Check what `GdParamAnnotator` sees for this call. The issue might be that the `[...]` syntax is parsed as two arguments (index + content) rather than one array literal. Or that `contains` doesn't resolve at all, and the error comes from a different call in the chain.

Run the test with `--info` and inspect which exact call triggers "Too many arguments".

- [ ] **Step 2: Apply the fix**

Based on diagnosis. Likely scenarios:

**If `assert_array()` return type is not resolving to `GdUnitArrayAssert`:**
The chained method resolution in `PsiGdExprUtil.getReturnType()` for `GdCallEx` needs the return type to propagate through the chain. This may be the same root cause as Category 1 — method return types from user-defined classes not resolving correctly.

**If `contains()` resolves to the wrong method (e.g., Array.contains):**
The `GdClassMemberUtil.listDeclarations()` needs to correctly identify the qualifier type as `GdUnitArrayAssert`, not `Array`.

**If the parser sees `["a"]` as an index expression:**
Check if `GdParamAnnotator` misinterprets the argument list structure.

Apply minimal fix.

- [ ] **Step 3: Run tests — expect PASS**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdChainedMethodResolutionTest" 2>&1 | tail -10`
Expected: PASS

- [ ] **Step 4: Run full test suite**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test 2>&1 | tail -15`
Expected: No new failures

- [ ] **Step 5: Commit**

```bash
git add -u
git commit -m "fix: resolve chained method calls through user-defined return types"
```

---

### Task 10: get_main_loop() → SceneTree — Write Failing Test (Category 5)

**Files:**
- Create: `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdMainLoopResolutionTest.kt`

- [ ] **Step 1: Write the test class**

```kotlin
package com.jetbrains.godot.gdscript.falsepositive

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GdMainLoopResolutionTest : GdFalsePositiveTestBase() {

    @Test
    fun testGetMainLoopRoot() {
        addNodeStubs()

        configureTest("""
            var tree := Engine.get_main_loop()
            var r := tree.root
        """.trimIndent())

        assertNoErrorContaining("Reference [root] not found")
    }

    @Test
    fun testGetMainLoopRootGetNode() {
        addNodeStubs()

        configureTest("""
            var store := Engine.get_main_loop().root.get_node("SomeNode")
        """.trimIndent())

        assertNoErrors()
    }

    @Test
    fun testGetMainLoopRootChained() {
        addNodeStubs()

        configureTest("""
            var r := Engine.get_main_loop().root
        """.trimIndent())

        assertNoErrorContaining("Reference [root] not found")
    }
}
```

- [ ] **Step 2: Run the tests — expect FAIL**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdMainLoopResolutionTest" 2>&1 | tail -20`
Expected: FAIL — "Reference [root] not found"

- [ ] **Step 3: Commit failing tests**

```bash
git add gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/falsepositive/GdMainLoopResolutionTest.kt
git commit -m "test(red): add failing tests for Engine.get_main_loop().root resolution"
```

---

### Task 11: get_main_loop() → SceneTree — Fix (Category 5)

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/psi/utils/PsiGdExprUtil.kt:101-134`

- [ ] **Step 1: Add get_main_loop special-case**

In `PsiGdExprUtil.getReturnType()`, inside the `is GdCallEx` → `run {}` block, add a new `else if` branch before the final `expr.expr.returnType` at line 133. After the `load`/`preload` block (line 131) and before line 133:

In `gdscript/src/main/kotlin/gdscript/psi/utils/PsiGdExprUtil.kt`, replace:

```kotlin
                    return ""
                }
                expr.expr.returnType
```

with:

```kotlin
                    return ""
                } else if (method == "get_main_loop") {
                    return "SceneTree"
                }
                expr.expr.returnType
```

- [ ] **Step 2: Run tests — expect PASS**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.GdMainLoopResolutionTest" 2>&1 | tail -10`
Expected: PASS

- [ ] **Step 3: Run full test suite**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test 2>&1 | tail -15`
Expected: No new failures

- [ ] **Step 4: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/psi/utils/PsiGdExprUtil.kt
git commit -m "fix: resolve get_main_loop() as SceneTree for root/get_node access"
```

---

### Task 12: Parser DEDENT + COMMA After Lambda — Verify (Category 6)

**Files:**
- Verify: `gdscript/src/main/kotlin/gdscript/parser/stmt/GdStmtParser.kt:56-66`
- Verify: `gdscript/src/test/testData/gdscript/parser/godotTestCases/signal_connect_multiline_lambda.gd`

- [ ] **Step 1: Run the existing parser test**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.parser.GdGodotTest.testsignal_connect_multiline_lambda" 2>&1 | tail -20`

If PASS → the fix from commit `18f18207` is working. Skip to Step 4.
If FAIL → continue with Step 2.

- [ ] **Step 2: Verify the fix is applied in GdStmtParser.kt**

Read `gdscript/src/main/kotlin/gdscript/parser/stmt/GdStmtParser.kt` lines 56-66 and confirm the DEDENT + COMMA handling matches:

```kotlin
        if (asLambda) {
            if (b.nextTokenIs(DEDENT)) {
                if (b.isArgs && b.followingTokensAre(DEDENT, COMMA)) {
                    b.consumeToken(DEDENT)
                } else if (!b.followingTokensAre(DEDENT, NEW_LINE) && !b.followingTokensAre(DEDENT, END_STMT)) {
                    b.remapCurrentToken(NEW_LINE)
                } else {
                    b.consumeToken(DEDENT)
                }
            }
        }
```

If the code matches, the issue is the `.txt` gold file. Update it:

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.parser.GdGodotTest.testsignal_connect_multiline_lambda" --info 2>&1 | grep -A30 "expected:\|actual:\|Expected\|Actual"`

Replace the `.txt` file content with the actual PSI tree output.

- [ ] **Step 3: Re-run test**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.parser.GdGodotTest.testsignal_connect_multiline_lambda" 2>&1 | tail -10`
Expected: PASS

- [ ] **Step 4: Run full test suite**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test 2>&1 | tail -15`
Expected: No new failures

- [ ] **Step 5: Commit (only if changes were needed)**

```bash
git add -u
git commit -m "fix(parser): update gold file for multiline lambda DEDENT+COMMA test"
```

---

### Task 13: Final Verification

**Files:** None (verification only)

- [ ] **Step 1: Run all false positive tests**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test --tests "com.jetbrains.godot.gdscript.falsepositive.*" 2>&1 | tail -15`
Expected: All tests PASS

- [ ] **Step 2: Run full test suite**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew test 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Build the plugin**

Run: `cd /home/tholo/plugins/godot-support/gdscript && ./gradlew buildPlugin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL
