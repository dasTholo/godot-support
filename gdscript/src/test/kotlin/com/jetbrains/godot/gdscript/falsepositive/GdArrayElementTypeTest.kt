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
