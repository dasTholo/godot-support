package com.jetbrains.godot.gdscript.dap.presenters

import gdscript.dap.presentation.presenters.ColorPresenter
import org.junit.Assert.*
import org.junit.Test

class ColorPresenterTest {

    private val presenter = ColorPresenter()

    @Test
    fun `canPresent matches Color type only`() {
        assertTrue(presenter.canPresent("Color"))
        assertFalse(presenter.canPresent("Vector2"))
        assertFalse(presenter.canPresent("ColorRect"))
        assertFalse(presenter.canPresent("int"))
    }

    @Test
    fun `formatValue wraps value with type`() {
        assertEquals("Color(1, 0, 0, 1)", presenter.formatValue("Color", "(1, 0, 0, 1)"))
    }

    @Test
    fun `formatValue handles already-prefixed value`() {
        assertEquals("Color(1, 0, 0, 1)", presenter.formatValue("Color", "Color(1, 0, 0, 1)"))
    }

    @Test
    fun `parseColor extracts RGBA from value string`() {
        val color = ColorPresenter.parseColor("(1, 0, 0.5, 0.8)")
        assertNotNull(color)
        assertEquals(255, color!!.red)
        assertEquals(0, color.green)
        assertEquals(127.0, color.blue.toDouble(), 1.0)
        assertEquals(204.0, color.alpha.toDouble(), 1.0)
    }

    @Test
    fun `parseColor returns null for invalid input`() {
        assertNull(ColorPresenter.parseColor("not a color"))
        assertNull(ColorPresenter.parseColor(""))
        assertNull(ColorPresenter.parseColor("(1, 2)"))
    }

    @Test
    fun `getIcon returns non-null for valid color`() {
        val icon = presenter.getIcon("Color", "(1, 0, 0, 1)")
        assertNotNull(icon)
        assertEquals(12, icon!!.iconWidth)
        assertEquals(12, icon.iconHeight)
    }

    @Test
    fun `getIcon returns null for unparseable value`() {
        assertNull(presenter.getIcon("Color", "invalid"))
    }
}
