# False Positive Fixes Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate remaining false positives by enriching GDExtension stubs with real Rust types, adding `...` varargs syntax support, and fixing inner-enum resolution.

**Architecture:** Three independent fixes: (1) Extend `GdExtensionRustResolver` to extract full method signatures from Rust source, map Rust types to GDScript equivalents, and generate typed stubs instead of `Variant`-based ones. (2) Add `ELLIPSIS` (`...`) token to the lexer and parameter parser so `...identifier` is recognized as vararg. (3) Extend the resolver's inner-class chain logic to also handle inner enums, allowing `Class.Enum.VALUE` access.

**Tech Stack:** Kotlin, JFlex, IntelliJ Platform SDK, JUnit 4

---

### Task 1: Extract Method Signatures from Rust Source

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/extension/GdExtensionRustResolver.kt`
- Modify: `gdscript/src/main/kotlin/gdscript/extension/GdExtensionTypeCollector.kt` (data class updates)

- [ ] **Step 1: Add `RustMethodInfo` and `RustMethodParam` data classes to `GdExtensionTypeCollector.kt`**

Add after the existing `GdExtSignalInfo` data class (line 24):

```kotlin
data class RustMethodParam(
    val name: String,
    val type: String
)

data class RustMethodInfo(
    val name: String,
    val params: List<RustMethodParam>,
    val returnType: String,
    val isStatic: Boolean
)
```

- [ ] **Step 2: Add `baseClass` field to `GdExtTypeInfo`**

Change the `GdExtTypeInfo` data class to add an optional `rustMethods` field:

```kotlin
data class GdExtTypeInfo(
    val name: String,
    val inherits: String,
    val methods: List<GdExtMethodInfo>,
    val properties: List<GdExtPropertyInfo>,
    val signals: List<GdExtSignalInfo>,
    val staticMethods: Set<String> = emptySet(),
    val rustMethods: List<RustMethodInfo>? = null
)
```

- [ ] **Step 3: Add type mapping and signature extraction to `GdExtensionRustResolver`**

Add these constants and methods to `GdExtensionRustResolver`:

```kotlin
// Add to companion object, after existing patterns:

// Matches fn name(&self, param: Type, param2: Type) -> ReturnType
// Captures: method name, full params string, optional return type
private val FUNC_FULL_PATTERN = Regex(
    """#\[func(?:\(([^)]*)\))?\]\s*(?:pub\s+)?fn\s+(\w+)\s*\(([^)]*)\)(?:\s*->\s*([^\s{]+(?:<[^>]+>)?))?""",
    RegexOption.MULTILINE
)

private val BASE_CLASS_PATTERN = Regex(
    """#\[class\([^)]*base\s*=\s*(\w+)""",
    RegexOption.MULTILINE
)

private val RUST_TO_GDSCRIPT = mapOf(
    "GString" to "String",
    "StringName" to "String",
    "bool" to "bool",
    "f32" to "float",
    "f64" to "float",
    "i8" to "int",
    "i16" to "int",
    "i32" to "int",
    "i64" to "int",
    "u8" to "int",
    "u16" to "int",
    "u32" to "int",
    "u64" to "int",
    "Variant" to "Variant",
    "VarDictionary" to "Dictionary",
    "PackedByteArray" to "PackedByteArray",
    "PackedInt32Array" to "PackedInt32Array",
    "PackedInt64Array" to "PackedInt64Array",
    "PackedFloat32Array" to "PackedFloat32Array",
    "PackedFloat64Array" to "PackedFloat64Array",
    "PackedStringArray" to "PackedStringArray",
    "PackedVector2Array" to "PackedVector2Array",
    "PackedVector3Array" to "PackedVector3Array",
    "PackedColorArray" to "PackedColorArray",
    "PackedVector4Array" to "PackedVector4Array",
    "Vector2" to "Vector2",
    "Vector2i" to "Vector2i",
    "Vector3" to "Vector3",
    "Vector3i" to "Vector3i",
    "Vector4" to "Vector4",
    "Vector4i" to "Vector4i",
    "Color" to "Color",
    "Rect2" to "Rect2",
    "Rect2i" to "Rect2i",
    "Transform2D" to "Transform2D",
    "Transform3D" to "Transform3D",
    "Basis" to "Basis",
    "Quaternion" to "Quaternion",
    "AABB" to "AABB",
    "Plane" to "Plane",
    "Projection" to "Projection",
    "RID" to "RID",
    "Callable" to "Callable",
    "Signal" to "Signal",
    "NodePath" to "NodePath",
)
```

And these methods:

```kotlin
fun mapRustType(rustType: String, className: String): String {
    // Direct mapping
    RUST_TO_GDSCRIPT[rustType]?.let { return it }

    // Gd<T> → T
    val gdMatch = Regex("""Gd<(\w+)>""").find(rustType)
    if (gdMatch != null) {
        val inner = gdMatch.groupValues[1]
        return if (inner == "Self") className else inner
    }

    // Option<Gd<T>> → T
    val optionGdMatch = Regex("""Option<Gd<(\w+)>>""").find(rustType)
    if (optionGdMatch != null) {
        val inner = optionGdMatch.groupValues[1]
        return if (inner == "Self") className else inner
    }

    // Array<Gd<T>> → Array[T]
    val arrayGdMatch = Regex("""Array<Gd<(\w+)>>""").find(rustType)
    if (arrayGdMatch != null) {
        val inner = arrayGdMatch.groupValues[1]
        return "Array[${if (inner == "Self") className else inner}]"
    }

    // Array<Variant> or Array<VarDictionary> → Array
    if (rustType.startsWith("Array<")) return "Array"

    // Dictionary<K,V> or VarDictionary → Dictionary
    if (rustType.startsWith("Dictionary<")) return "Dictionary"

    // Unknown → Variant
    return "Variant"
}

fun collectMethodSignatures(className: String): List<RustMethodInfo> {
    val mapping = buildClassNameMapping()
    val location = mapping[className] ?: return emptyList()

    val content = try {
        String(location.virtualFile.contentsToByteArray(), Charsets.UTF_8)
    } catch (_: Exception) {
        return emptyList()
    }

    val methods = mutableListOf<RustMethodInfo>()
    FUNC_FULL_PATTERN.findAll(content).forEach { match ->
        val funcAttr = match.groupValues[1]  // content inside #[func(...)]
        val methodName = match.groupValues[2]
        val paramsStr = match.groupValues[3].trim()
        val returnStr = match.groupValues[4].trim()

        // Check for #[func(rename = new_name)]
        val renameMatch = Regex("""rename\s*=\s*(\w+)""").find(funcAttr)
        val gdMethodName = renameMatch?.groupValues?.get(1) ?: methodName

        // Parse parameters, skip &self/&mut self
        val params = mutableListOf<RustMethodParam>()
        var isStatic = true
        if (paramsStr.isNotEmpty()) {
            val paramParts = splitRustParams(paramsStr)
            for (part in paramParts) {
                val trimmed = part.trim()
                if (trimmed == "&self" || trimmed == "&mut self" || trimmed == "self") {
                    isStatic = false
                    continue
                }
                val colonIdx = trimmed.indexOf(':')
                if (colonIdx > 0) {
                    val paramName = trimmed.substring(0, colonIdx).trim()
                    val paramType = trimmed.substring(colonIdx + 1).trim()
                    params.add(RustMethodParam(paramName, mapRustType(paramType, className)))
                }
            }
        }

        val gdReturnType = if (returnStr.isEmpty()) "void" else mapRustType(returnStr, className)

        methods.add(RustMethodInfo(gdMethodName, params, gdReturnType, isStatic))
    }

    return methods
}

fun getBaseClass(className: String): String? {
    val mapping = buildClassNameMapping()
    val location = mapping[className] ?: return null

    val content = try {
        String(location.virtualFile.contentsToByteArray(), Charsets.UTF_8)
    } catch (_: Exception) {
        return null
    }

    return BASE_CLASS_PATTERN.find(content)?.groupValues?.get(1)
}

/**
 * Split Rust parameter list respecting nested angle brackets.
 */
private fun splitRustParams(params: String): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var start = 0
    for (i in params.indices) {
        when (params[i]) {
            '<' -> depth++
            '>' -> depth--
            ',' -> if (depth == 0) {
                result.add(params.substring(start, i))
                start = i + 1
            }
        }
    }
    if (start < params.length) result.add(params.substring(start))
    return result
}
```

- [ ] **Step 4: Verify the file compiles**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/extension/GdExtensionRustResolver.kt gdscript/src/main/kotlin/gdscript/extension/GdExtensionTypeCollector.kt
git commit -m "feat(gdext): extract full method signatures and type mapping from Rust source"
```

---

### Task 2: Generate Typed Stubs from Rust Data

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubService.kt:48-55`
- Modify: `gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubWriter.kt:29-57`

- [ ] **Step 1: Update `GdExtensionStubService.generateStubs()` to use Rust method signatures**

Replace the current enrichment block (lines 50-55) with logic that collects full signatures and base class:

```kotlin
// Step 3b: Enrich with Rust source data (full signatures + base class + static methods)
val rustResolver = GdExtensionRustResolver.getInstance(project)
val enrichedTypeInfos = typeInfos.map { type ->
    val rustMethods = rustResolver.collectMethodSignatures(type.name)
    val baseClass = rustResolver.getBaseClass(type.name)
    val staticMethods = rustResolver.collectStaticMethods(type.name)
    type.copy(
        inherits = baseClass ?: type.inherits,
        staticMethods = staticMethods,
        rustMethods = rustMethods.ifEmpty { null }
    )
}
```

- [ ] **Step 2: Update `GdExtensionStubWriter.generateStub()` to prefer Rust methods**

Replace the methods section of `generateStub()` (lines 49-54) with:

```kotlin
// Methods — prefer Rust-sourced signatures when available
if (type.rustMethods != null) {
    for (method in type.rustMethods) {
        val params = method.params.joinToString(", ") { "${it.name}: ${it.type}" }
        val ret = " -> ${method.returnType}"
        val prefix = if (method.isStatic) "static func" else "func"
        sb.appendLine("$prefix ${method.name}($params)$ret: pass")
    }
} else {
    for (method in type.methods) {
        val params = method.params.joinToString(", ") { "${it.first}: ${it.second}" }
        val ret = if (method.returnType == "void") " -> void" else " -> ${method.returnType}"
        val prefix = if (method.name in type.staticMethods) "static func" else "func"
        sb.appendLine("$prefix ${method.name}($params)$ret: pass")
    }
}
```

- [ ] **Step 3: Verify the file compiles**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubService.kt gdscript/src/main/kotlin/gdscript/extension/GdExtensionStubWriter.kt
git commit -m "feat(gdext): generate typed stubs from Rust source, fall back to LSP Variant stubs"
```

---

### Task 3: Add ELLIPSIS Token to Lexer

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/Gd.flex:256`
- Modify: `gdscript/src/main/kotlin/gdscript/Gd.bnf:135,138`

- [ ] **Step 1: Add `...` token to `Gd.flex`**

Add the `...` token **before** the `..` token on line 256 (JFlex matches longest first but explicit ordering is safer):

```
    "..."          { return dedentRoot(GdTypes.ELLIPSIS); }
    ".."           { return dedentRoot(GdTypes.DOTDOT); }
```

- [ ] **Step 2: Add ELLIPSIS to the BNF grammar**

In `Gd.bnf`, update the `param` rule (line 138) to accept an optional ELLIPSIS prefix:

```
param ::= VAR? ELLIPSIS? var_nmi typed? (assignTyped expr)? {methods=[getReturnType isVararg]}
```

Also add ELLIPSIS as an allowed method specifier alternative — no, wait. The `...` prefix is a parameter-level feature, not a method specifier. The BNF change above is sufficient.

- [ ] **Step 3: Regenerate the lexer**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew generateLexer 2>&1 | tail -5`

If `generateLexer` is not a Gradle task, regenerate manually:

Run: `cd /home/tholo/plugins/godot-support && ./gradlew generateParser 2>&1 | tail -10`

Check the generated `GdLexer.java` has the ELLIPSIS handling.

- [ ] **Step 4: Verify the lexer compiles**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew compileJava 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/Gd.flex gdscript/src/main/kotlin/gdscript/Gd.bnf gdscript/src/main/gen/gdscript/GdLexer.java
git commit -m "feat(parser): add ELLIPSIS token for varargs prefix syntax"
```

---

### Task 4: Support `...param` as Vararg in Parameter Parser

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/parser/common/GdParamListParser.kt`
- Modify: `gdscript/src/main/kotlin/gdscript/psi/utils/PsiGdMethodDeclUtil.kt:15-22`

- [ ] **Step 1: Update `GdParamListParser.param()` to consume ELLIPSIS**

The BNF change from Task 3 adds `ELLIPSIS?` before `var_nmi` in the `param` rule. The hand-written parser needs to match. Update `GdParamListParser.param()`:

```kotlin
private fun param(b: GdPsiBuilder, l: Int): Boolean {
    if (!b.recursionGuard(l, "Param")) return false
    val param = b.mark()
    var ok = true

    b.passToken(VAR)
    b.passToken(ELLIPSIS)  // optional varargs prefix
    ok = ok && GdLiteralExParser.parseExtendedRefId(b, VAR_NMI)
    ok = ok && GdTypedParser.parseWithAssignTypedAndExpr(b, l + 1, true)

    param.done(PARAM)

    return ok
}
```

Also update the `parse()` method's lookahead to recognize ELLIPSIS as a valid param start:

```kotlin
override fun parse(b: GdPsiBuilder, l: Int, optional: Boolean): Boolean {
    if (!b.recursionGuard(l, "ParamList")) return false
    if (!b.nextTokenIs(VAR) && !b.nextTokenIs(ELLIPSIS) && !GdLiteralExParser.checkExtendedRefId(b)) return optional
    var ok = true
    val paramList = b.mark()

    while (ok && (b.nextTokenIs(VAR) || b.nextTokenIs(ELLIPSIS) || GdLiteralExParser.checkExtendedRefId(b))) {
        ok = param(b, l + 1)
        if (!b.passToken(COMMA)) break
    }

    paramList.done(PARAM_LIST)

    return true
}
```

- [ ] **Step 2: Update `isVariadic` to detect `...` prefix on any parameter**

In `PsiGdMethodDeclUtil.kt`, extend `isVariadic()` to also check for ELLIPSIS in parameters:

```kotlin
fun isVariadic(element: GdMethodDeclTl): Boolean {
    val stub = element.stub
    if (stub !== null) {
        return stub.isVariadic()
    }

    // Classic vararg keyword
    if (element.methodSpecifierList.any { it.text == GdKeywords.VARARG }) return true

    // Ellipsis prefix on any parameter (e.g., func foo(...args: Array))
    val params = element.paramList?.paramList ?: return false
    return params.any { param ->
        param.node.getChildren(null).any { it.elementType == GdTypes.ELLIPSIS }
    }
}
```

- [ ] **Step 3: Verify the file compiles**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/parser/common/GdParamListParser.kt gdscript/src/main/kotlin/gdscript/psi/utils/PsiGdMethodDeclUtil.kt
git commit -m "feat(parser): recognize ...param as vararg in parameter declarations"
```

---

### Task 5: Write Varargs Parser Test

**Files:**
- Create: `gdscript/src/test/testData/gdscript/parser/data/MethodEllipsisVararg.gd`
- Create: `gdscript/src/test/testData/gdscript/parser/data/MethodEllipsisVararg.txt`
- Modify: `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/parser/GdParserTest.kt`

- [ ] **Step 1: Create the test GDScript file**

Create `gdscript/src/test/testData/gdscript/parser/data/MethodEllipsisVararg.gd`:

```gdscript
func contains(...expected: Array):
    pass
```

- [ ] **Step 2: Generate the expected PSI tree**

Run the parser test once to get the actual output, then use it as the expected `.txt` file. First, add the test method to `GdParserTest.kt` (after the `testMethodVararg` line):

```kotlin
@Test fun testMethodEllipsisVararg() = doTest(true)
```

- [ ] **Step 3: Run the test to get actual output**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew test --tests "com.jetbrains.godot.gdscript.parser.GdParserTest.testMethodEllipsisVararg" 2>&1 | tail -20`

The test will fail with "Mismatch" and show the actual PSI tree. Copy the actual output into `MethodEllipsisVararg.txt`.

- [ ] **Step 4: Verify the PSI tree contains ELLIPSIS**

The `.txt` file should contain a node for the ELLIPSIS token inside the PARAM element. Verify this manually.

- [ ] **Step 5: Re-run the test to confirm it passes**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew test --tests "com.jetbrains.godot.gdscript.parser.GdParserTest.testMethodEllipsisVararg" 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add gdscript/src/test/testData/gdscript/parser/data/MethodEllipsisVararg.gd gdscript/src/test/testData/gdscript/parser/data/MethodEllipsisVararg.txt gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/parser/GdParserTest.kt
git commit -m "test(parser): add parser test for ellipsis vararg syntax"
```

---

### Task 6: Fix Inner Enum Resolution in `GdClassMemberReference`

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/reference/GdClassMemberReference.kt:76-93,141-187`

- [ ] **Step 1: Extend `resolvesToClassChain` to also resolve enum chains**

In `GdClassMemberReference.resolveDeclaration()`, the `resolvesToClassChain` lambda (lines 76-93) only walks `GdClassDeclTl`. Extend it to also check if the final segment is an enum. Replace the entire lambda:

```kotlin
val resolvesToClassChain = fun(name: String): Boolean {
    if (name.isEmpty()) return false
    if (GdClassUtil.getClassIdElement(name, element, element.project) != null) return true
    val parts = name.split('.')
    if (parts.isEmpty()) return false
    var parent: PsiElement = GdClassUtil.getOwningClassElement(element)
    parent = parent as? GdFile ?: element.containingFile
    var current: PsiElement? = PsiTreeUtil.getStubChildrenOfTypeAsList(parent, GdClassDeclTl::class.java)
        .firstOrNull { it.name == parts[0] }
    // Also check if the first part is an enum at file/class scope
    if (current == null) {
        current = PsiTreeUtil.getStubChildrenOfTypeAsList(parent, GdEnumDeclTl::class.java)
            .firstOrNull { it.name == parts[0] }
        if (current != null) return parts.size == 1  // Enums don't nest further
    }
    var i = 1
    while (current != null && current is GdClassDeclTl && i < parts.size) {
        // Try inner class first
        val nextClass = PsiTreeUtil.getStubChildrenOfTypeAsList(current, GdClassDeclTl::class.java)
            .firstOrNull { it.name == parts[i] }
        if (nextClass != null) {
            current = nextClass
            i++
            continue
        }
        // Try inner enum (terminal — enums don't contain classes)
        val nextEnum = PsiTreeUtil.getStubChildrenOfTypeAsList(current, GdEnumDeclTl::class.java)
            .firstOrNull { it.name == parts[i] }
        if (nextEnum != null) {
            current = nextEnum
            i++
            break  // Enum is always terminal in the chain
        }
        break
    }
    return current != null && i == parts.size
}
```

- [ ] **Step 2: Allow inner enums through the static/instance access filter**

In the ownership validation block (lines 157-188), add `GdEnumDeclTl` alongside `GdClassDeclTl` as valid resolutions. After the `is GdClassDeclTl` block (line 168-171), add:

```kotlin
is GdEnumDeclTl -> {
    // Enums are always accessible statically (like classes)
    if (isStaticAccess == false) return@Resolver null
}
```

And in the inner-class access block (lines 174-187), extend to handle inner enums:

```kotlin
// Allow accessing inner classes AND inner enums via their direct parent class
if (resolved is GdClassDeclTl || resolved is GdEnumDeclTl) {
    val enclosing = PsiTreeUtil.getStubOrPsiParentOfType(resolved, GdClassDeclTl::class.java)
    if (enclosing != null && enclosing == targetClassDecl) {
        // OK: accessing inner class/enum on its parent
    } else if (owner is GdClassDeclTl && owner != targetClassDecl) {
        return@Resolver null
    }
} else {
    // For non-class members, require exact owning class match
    if (owner is GdClassDeclTl && owner != targetClassDecl) {
        return@Resolver null
    }
}
```

Also add `GdEnumDeclTl` to the `qualifierQualifiesAsClass` guard block (lines 148-155):

```kotlin
if (qualifierExpr != null && qualifierQualifiesAsClass(qualifierExpr)) {
    when (resolved) {
        is GdClassDeclTl -> { /* ok */ }
        is GdEnumDeclTl -> { /* ok — enums are static-like */ }
        is GdMethodDeclTl -> if (!resolved.isStatic) return@Resolver null
        is GdClassVarDeclTl -> if (!resolved.isStatic) return@Resolver null
    }
}
```

Make sure `GdEnumDeclTl` is imported at the top of the file.

- [ ] **Step 3: Verify the file compiles**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/reference/GdClassMemberReference.kt
git commit -m "fix(resolver): allow inner enum resolution via Class.Enum.VALUE chain"
```

---

### Task 7: Fix Inner Enum in Annotator

**Files:**
- Modify: `gdscript/src/main/kotlin/gdscript/annotator/GdRefIdAnnotator.kt:56,90-98`

- [ ] **Step 1: Add `GdEnumDeclTl` as a valid resolved type in the annotator**

In `GdRefIdAnnotator.annotate()`, the `when` block for `resolved` (line 49) handles `GdClassDeclTl` at line 56. Add `GdEnumDeclTl` as a peer:

```kotlin
is GdEnumDeclTl -> GdHighlighterColors.CLASS_TYPE
```

Add this case after the `is PsiFile, is GdClassDeclTl, is GdClassNaming` case (line 56). The full block becomes:

```kotlin
is PsiFile, is GdClassDeclTl, is GdClassNaming -> {
    // ... existing code ...
}

is GdEnumDeclTl -> GdHighlighterColors.CLASS_TYPE
```

- [ ] **Step 2: Extend enum member lookup to handle inner enum chains**

In the `null` branch (line 76), the block at lines 91-98 checks if the qualifier resolves to a named enum. This already works for `_Anim.FLOOR` but it also needs to work when `findDeclaration(calledUponExpr)` returns `null` for inner enums accessed via class chain.

The existing code at lines 91-98 should work as-is because `GdClassMemberUtil.findDeclaration(calledUponExpr)` will resolve `EquipmentPanel.SlotPosition` to the `GdEnumDeclTl` now that the reference resolver (Task 6) supports inner enums. The annotator then checks enum values correctly.

No change needed here — verify via test.

- [ ] **Step 3: Verify import for `GdEnumDeclTl`**

Ensure `gdscript.psi.GdEnumDeclTl` is imported. The wildcard import `gdscript.psi.*` at line 14 already covers it.

- [ ] **Step 4: Verify the file compiles**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gdscript/src/main/kotlin/gdscript/annotator/GdRefIdAnnotator.kt
git commit -m "fix(annotator): highlight inner enums as types, not unresolved references"
```

---

### Task 8: Write Inner Enum Resolution Test

**Files:**
- Create: `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/resolve/ResolveInnerEnumTest.kt`

- [ ] **Step 1: Write the test**

Create `gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/resolve/ResolveInnerEnumTest.kt`:

```kotlin
package com.jetbrains.godot.gdscript.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import gdscript.psi.GdEnumDeclTl
import gdscript.psi.GdEnumValue
import gdscript.psi.GdRefIdRef
import gdscript.reference.GdClassMemberReference

class ResolveInnerEnumTest : BasePlatformTestCase() {

    fun testResolveInnerEnumViaClassDot() {
        val code = """
            |class_name EquipmentPanel
            |
            |enum SlotPosition { NORTH, SOUTH, EAST, WEST }
            |
            |func use():
            |    var pos = EquipmentPanel.SlotPosition.NORTH
        """.trimMargin()

        val file = myFixture.configureByText("EquipmentPanel.gd", code)

        // Resolve "SlotPosition" in the chain
        val offsetSlot = file.text.indexOf("SlotPosition.NORTH")
        val elementSlot = file.findElementAt(offsetSlot)!!.parent as GdRefIdRef
        val declSlot = GdClassMemberReference(elementSlot).resolveDeclaration()
        assertNotNull("SlotPosition should resolve to inner enum", declSlot)
        assertTrue("Should resolve to GdEnumDeclTl", declSlot is GdEnumDeclTl)
        assertEquals("SlotPosition", (declSlot as GdEnumDeclTl).name)

        // Resolve "NORTH" in the chain
        val offsetNorth = file.text.indexOf("NORTH", file.text.indexOf("var pos"))
        val elementNorth = file.findElementAt(offsetNorth)!!.parent as GdRefIdRef
        val declNorth = GdClassMemberReference(elementNorth).resolveDeclaration()
        assertNotNull("NORTH should resolve to enum value", declNorth)
        assertTrue("Should resolve to GdEnumValue", declNorth is GdEnumValue)
    }

    fun testResolveInnerEnumInNestedClass() {
        val code = """
            |class A:
            |    class B:
            |        enum Direction { UP, DOWN }
            |
            |func use():
            |    var d = A.B.Direction.UP
        """.trimMargin()

        val file = myFixture.configureByText("Nested.gd", code)

        val offsetDir = file.text.indexOf("Direction.UP")
        val elementDir = file.findElementAt(offsetDir)!!.parent as GdRefIdRef
        val declDir = GdClassMemberReference(elementDir).resolveDeclaration()
        assertNotNull("Direction should resolve inside nested class", declDir)
        assertTrue(declDir is GdEnumDeclTl)

        val offsetUp = file.text.indexOf("UP", file.text.indexOf("var d"))
        val elementUp = file.findElementAt(offsetUp)!!.parent as GdRefIdRef
        val declUp = GdClassMemberReference(elementUp).resolveDeclaration()
        assertNotNull("UP should resolve to enum value", declUp)
        assertTrue(declUp is GdEnumValue)
    }
}
```

- [ ] **Step 2: Run the test**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew test --tests "com.jetbrains.godot.gdscript.resolve.ResolveInnerEnumTest" 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL, 2 tests passed

- [ ] **Step 3: Commit**

```bash
git add gdscript/src/test/kotlin/com/jetbrains/godot/gdscript/resolve/ResolveInnerEnumTest.kt
git commit -m "test(resolver): add tests for inner enum resolution via Class.Enum.VALUE"
```

---

### Task 9: Run Full Test Suite

**Files:** None (verification only)

- [ ] **Step 1: Run all parser tests**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew test --tests "com.jetbrains.godot.gdscript.parser.*" 2>&1 | tail -10`
Expected: All tests pass

- [ ] **Step 2: Run all resolve tests**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew test --tests "com.jetbrains.godot.gdscript.resolve.*" 2>&1 | tail -10`
Expected: All tests pass

- [ ] **Step 3: Run all red code tests**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew test --tests "com.jetbrains.godot.gdscript.redCode.*" 2>&1 | tail -10`
Expected: All tests pass

- [ ] **Step 4: Run full test suite**

Run: `cd /home/tholo/plugins/godot-support && ./gradlew test 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL
