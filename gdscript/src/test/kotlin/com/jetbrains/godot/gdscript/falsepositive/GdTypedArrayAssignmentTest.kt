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
