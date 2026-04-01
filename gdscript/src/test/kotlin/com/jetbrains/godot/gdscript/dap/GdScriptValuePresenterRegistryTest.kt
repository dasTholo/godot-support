package com.jetbrains.godot.gdscript.dap

import gdscript.dap.presentation.GdScriptValuePresenter
import gdscript.dap.presentation.GdScriptValuePresenterRegistry
import gdscript.dap.presentation.presenters.DefaultPresenter
import org.junit.Assert.*
import org.junit.Test
import javax.swing.Icon

class GdScriptValuePresenterRegistryTest {

    private class StubPresenter(private val typeMatch: String) : GdScriptValuePresenter {
        override fun canPresent(type: String): Boolean = type == typeMatch
        override fun formatValue(type: String, value: String): String = "formatted:$value"
        override fun getIcon(type: String, value: String): Icon? = null
    }

    @Test
    fun `find returns matching presenter`() {
        val registry = GdScriptValuePresenterRegistry(
            listOf(StubPresenter("Color"), StubPresenter("Vector2"))
        )
        val presenter = registry.find("Color")
        assertNotNull(presenter)
        assertEquals("formatted:red", presenter!!.formatValue("Color", "red"))
    }

    @Test
    fun `find returns null for unknown type`() {
        val registry = GdScriptValuePresenterRegistry(
            listOf(StubPresenter("Color"))
        )
        assertNull(registry.find("UnknownType"))
    }

    @Test
    fun `find returns first matching presenter`() {
        val first = StubPresenter("Color")
        val second = StubPresenter("Color")
        val registry = GdScriptValuePresenterRegistry(listOf(first, second))
        assertSame(first, registry.find("Color"))
    }

    @Test
    fun `DefaultPresenter accepts any type`() {
        val presenter = DefaultPresenter()
        assertTrue(presenter.canPresent("int"))
        assertTrue(presenter.canPresent("Color"))
        assertTrue(presenter.canPresent("SomeRandomType"))
    }

    @Test
    fun `DefaultPresenter returns value as-is`() {
        val presenter = DefaultPresenter()
        assertEquals("(1, 0, 0, 1)", presenter.formatValue("Color", "(1, 0, 0, 1)"))
    }

    @Test
    fun `DefaultPresenter returns null icon`() {
        val presenter = DefaultPresenter()
        assertNull(presenter.getIcon("int", "42"))
    }
}
