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
