package com.jetbrains.godot.gdscript.dap.lldb

import org.junit.Assert.*
import org.junit.Test

class GdExtLldbFormattersTest {

    @Test
    fun `formatter file exists in resources`() {
        val resource = javaClass.classLoader.getResource("debugger/gdext_lldb_formatters.py")
        assertNotNull("gdext_lldb_formatters.py should be in resources", resource)
    }

    @Test
    fun `formatter file contains required summary providers`() {
        val content = javaClass.classLoader.getResource("debugger/gdext_lldb_formatters.py")!!.readText()
        assertTrue("Should contain GString formatter", content.contains("GString"))
        assertTrue("Should contain Vector2 formatter", content.contains("Vector2"))
        assertTrue("Should contain Gd<T> formatter", content.contains("summary_gd_object"))
        assertTrue("Should contain Color formatter", content.contains("Color"))
        assertTrue("Should contain register function", content.contains("def __lldb_init_module"))
    }
}
