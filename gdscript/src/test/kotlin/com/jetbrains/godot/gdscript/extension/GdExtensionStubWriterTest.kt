package com.jetbrains.godot.gdscript.extension

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import gdscript.extension.GdExtMethodInfo
import gdscript.extension.GdExtPropertyInfo
import gdscript.extension.GdExtSignalInfo
import gdscript.extension.GdExtTypeInfo
import gdscript.extension.GdExtensionStubWriter
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files

@RunWith(JUnit4::class)
class GdExtensionStubWriterTest : BasePlatformTestCase() {

    @Test
    fun testStubWriterFiltersInheritedMembers() {
        // Set up SDK stub as an indexed file in the project
        myFixture.addFileToProject("sdk/Animal.gd", """
            extends RefCounted
            class_name Animal

            var health: int = 100
            func move() -> void: pass
            signal died()
        """.trimIndent())

        // GDExtension type that extends Animal
        val types = listOf(
            GdExtTypeInfo(
                name = "Dog",
                inherits = "Animal",
                methods = listOf(
                    GdExtMethodInfo("move", emptyList(), "void"),
                    GdExtMethodInfo("bark", emptyList(), "void"),
                ),
                properties = listOf(
                    GdExtPropertyInfo("health", "int"),
                    GdExtPropertyInfo("breed", "String"),
                ),
                signals = listOf(
                    GdExtSignalInfo("died"),
                    GdExtSignalInfo("barked"),
                ),
            ),
        )

        val outputDir = Files.createTempDirectory("gdext-stubs-test")
        try {
            GdExtensionStubWriter.writeStubs(types, outputDir, null, project)
            val content = outputDir.resolve("Dog.gd").toFile().readText()

            // Inherited members should be filtered out
            assertFalse("Should not contain inherited method 'move'", content.contains("func move("))
            assertFalse("Should not contain inherited var 'health'", content.contains("var health"))
            assertFalse("Should not contain inherited signal 'died'", content.contains("signal died"))

            // Own members should be present
            assertTrue("Should contain own method 'bark'", content.contains("func bark("))
            assertTrue("Should contain own var 'breed'", content.contains("var breed"))
            assertTrue("Should contain own signal 'barked'", content.contains("signal barked"))
        } finally {
            outputDir.toFile().deleteRecursively()
        }
    }
}
