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
