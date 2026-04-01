package com.jetbrains.godot.gdscript.dap.presenters

import gdscript.dap.presentation.presenters.NodePresenter
import org.junit.Assert.*
import org.junit.Test

class NodePresenterTest {

    private val presenter = NodePresenter()

    @Test
    fun `canPresent matches Node types`() {
        assertTrue(presenter.canPresent("Node"))
        assertTrue(presenter.canPresent("Node2D"))
        assertTrue(presenter.canPresent("Node3D"))
        assertTrue(presenter.canPresent("CharacterBody2D"))
        assertTrue(presenter.canPresent("Sprite2D"))
        assertTrue(presenter.canPresent("Camera2D"))
        assertTrue(presenter.canPresent("Control"))
        assertTrue(presenter.canPresent("Label"))
        assertTrue(presenter.canPresent("Button"))
        assertTrue(presenter.canPresent("AnimationPlayer"))
    }

    @Test
    fun `canPresent rejects non-node types`() {
        assertFalse(presenter.canPresent("Vector2"))
        assertFalse(presenter.canPresent("Color"))
        assertFalse(presenter.canPresent("int"))
        assertFalse(presenter.canPresent("String"))
        assertFalse(presenter.canPresent("Resource"))
        assertFalse(presenter.canPresent("Array"))
    }

    @Test
    fun `formatValue extracts instance info`() {
        val result = presenter.formatValue("CharacterBody2D", "<CharacterBody2D#1234>")
        assertEquals("CharacterBody2D #1234", result)
    }

    @Test
    fun `formatValue handles object reference format`() {
        val result = presenter.formatValue("Node2D", "<Node2D#5678>")
        assertEquals("Node2D #5678", result)
    }

    @Test
    fun `formatValue falls back to type + raw value`() {
        val result = presenter.formatValue("Sprite2D", "some-value")
        assertEquals("Sprite2D: some-value", result)
    }

    @Test
    fun `getIcon returns non-null`() {
        val icon = presenter.getIcon("CharacterBody2D", "<CharacterBody2D#1234>")
        assertNotNull(icon)
    }
}
