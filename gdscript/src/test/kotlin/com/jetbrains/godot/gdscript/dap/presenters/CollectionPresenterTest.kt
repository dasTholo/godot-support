package com.jetbrains.godot.gdscript.dap.presenters

import gdscript.dap.presentation.presenters.CollectionPresenter
import org.junit.Assert.*
import org.junit.Test

class CollectionPresenterTest {

    private val presenter = CollectionPresenter()

    @Test
    fun `canPresent matches Array and Dictionary`() {
        assertTrue(presenter.canPresent("Array"))
        assertTrue(presenter.canPresent("Dictionary"))
    }

    @Test
    fun `canPresent rejects other types`() {
        assertFalse(presenter.canPresent("PackedByteArray"))
        assertFalse(presenter.canPresent("String"))
        assertFalse(presenter.canPresent("Node"))
    }

    @Test
    fun `formatValue for Array with element count`() {
        assertEquals("Array (3 elements)", presenter.formatValue("Array", "[1, 2, 3]"))
    }

    @Test
    fun `formatValue for empty Array`() {
        assertEquals("Array (empty)", presenter.formatValue("Array", "[]"))
    }

    @Test
    fun `formatValue for Dictionary with entry count`() {
        assertEquals("Dictionary (2 entries)", presenter.formatValue("Dictionary", "{a:1, b:2}"))
    }

    @Test
    fun `formatValue for empty Dictionary`() {
        assertEquals("Dictionary (empty)", presenter.formatValue("Dictionary", "{}"))
    }

    @Test
    fun `formatValue falls back on unparseable value`() {
        assertEquals("Array: <null>", presenter.formatValue("Array", "<null>"))
    }

    @Test
    fun `getIcon returns null`() {
        assertNull(presenter.getIcon("Array", "[1,2]"))
    }
}
