package com.jetbrains.godot.gdscript.dap.presenters

import gdscript.dap.presentation.presenters.VectorPresenter
import org.junit.Assert.*
import org.junit.Test

class VectorPresenterTest {

    private val presenter = VectorPresenter()

    @Test
    fun `canPresent matches all vector-like types`() {
        val supported = listOf(
            "Vector2", "Vector2i", "Vector3", "Vector3i", "Vector4", "Vector4i",
            "Rect2", "Rect2i", "AABB", "Plane", "Quaternion"
        )
        for (type in supported) {
            assertTrue("Should support $type", presenter.canPresent(type))
        }
    }

    @Test
    fun `canPresent rejects non-vector types`() {
        assertFalse(presenter.canPresent("Color"))
        assertFalse(presenter.canPresent("Transform2D"))
        assertFalse(presenter.canPresent("Node2D"))
        assertFalse(presenter.canPresent("int"))
    }

    @Test
    fun `formatValue prefixes type name`() {
        assertEquals("Vector2(1.5, 3.2)", presenter.formatValue("Vector2", "(1.5, 3.2)"))
        assertEquals("Vector3i(1, 2, 3)", presenter.formatValue("Vector3i", "(1, 2, 3)"))
        assertEquals("AABB(0, 0, 0, 1, 1, 1)", presenter.formatValue("AABB", "(0, 0, 0, 1, 1, 1)"))
    }

    @Test
    fun `formatValue does not double-prefix`() {
        assertEquals("Vector2(1.5, 3.2)", presenter.formatValue("Vector2", "Vector2(1.5, 3.2)"))
    }

    @Test
    fun `getIcon returns null`() {
        assertNull(presenter.getIcon("Vector2", "(1, 2)"))
    }
}
