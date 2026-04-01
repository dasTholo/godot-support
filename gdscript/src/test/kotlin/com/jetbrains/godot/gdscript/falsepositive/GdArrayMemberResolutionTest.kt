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
