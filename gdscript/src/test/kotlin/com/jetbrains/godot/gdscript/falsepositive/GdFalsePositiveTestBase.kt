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
