package com.jetbrains.godot.gdscript.dap.presenters

import gdscript.dap.presentation.presenters.TransformPresenter
import org.junit.Assert.*
import org.junit.Test

class TransformPresenterTest {

    private val presenter = TransformPresenter()

    @Test
    fun `canPresent matches transform types`() {
        assertTrue(presenter.canPresent("Transform2D"))
        assertTrue(presenter.canPresent("Transform3D"))
        assertTrue(presenter.canPresent("Basis"))
    }

    @Test
    fun `canPresent rejects non-transform types`() {
        assertFalse(presenter.canPresent("Vector2"))
        assertFalse(presenter.canPresent("Color"))
    }

    @Test
    fun `formatValue for Transform2D`() {
        val result = presenter.formatValue("Transform2D", "((1, 0), (0, 1), (0, 0))")
        assertEquals("Transform2D(x: (1, 0), y: (0, 1), origin: (0, 0))", result)
    }

    @Test
    fun `formatValue for Transform3D`() {
        val result = presenter.formatValue("Transform3D", "((1, 0, 0), (0, 1, 0), (0, 0, 1), (0, 0, 0))")
        assertEquals("Transform3D(x: (1, 0, 0), y: (0, 1, 0), z: (0, 0, 1), origin: (0, 0, 0))", result)
    }

    @Test
    fun `formatValue for Basis`() {
        val result = presenter.formatValue("Basis", "((1, 0, 0), (0, 1, 0), (0, 0, 1))")
        assertEquals("Basis(x: (1, 0, 0), y: (0, 1, 0), z: (0, 0, 1))", result)
    }

    @Test
    fun `formatValue returns raw value on parse failure`() {
        val result = presenter.formatValue("Transform2D", "invalid")
        assertEquals("Transform2D: invalid", result)
    }

    @Test
    fun `getIcon returns null`() {
        assertNull(presenter.getIcon("Transform2D", "((1,0),(0,1),(0,0))"))
    }
}
