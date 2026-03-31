package sdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ClassParserTest {

    @Test
    fun `parses simple class with all features`(@TempDir tempDir: File) {
        val xmlDir = tempDir.resolve("doc/classes").also { it.mkdirs() }
        val inputXml = File(javaClass.classLoader.getResource("class_simple.xml")!!.toURI())
        inputXml.copyTo(xmlDir.resolve("MyClass.xml"))

        val outputDir = tempDir.resolve("output").also { it.mkdirs() }

        ClassParser.parse(listOf(xmlDir), outputDir)

        val output = outputDir.resolve("MyClass.gd")
        val content = output.readText()

        // Check class header
        assert("extends RefCounted\n" in content)
        assert("class_name MyClass\n" in content)

        // Check documentation
        assert("## A simple class.\n" in content)
        assert("## This is the full description.\n" in content)
        assert("## @tutorial(My Tutorial): https://docs.godotengine.org/en/stable/tutorials/my_tutorial.html\n" in content)
        assert("## @deprecated\n" in content)

        // Check signal with params
        assert("signal value_changed(new_value: int)\n" in content)

        // Check constant
        assert("const MAX_SIZE = 100;\n" in content)

        // Check enum (unnamed block)
        assert("#enum MyEnum\nenum {\n    FLAG_A = 1,\n    FLAG_B = 2,\n}\n" in content)
        // Check enum (named block)
        assert("#enum MyEnum\nenum MyEnum {\n    FLAG_A = 1,\n    FLAG_B = 2,\n}\n" in content)

        // Check member with getter/setter
        assert("var speed: float:\n\tget = get_speed, set = set_speed\n" in content)

        // Check constructor
        assert("func MyClass(value: int = 0) -> MyClass:\n\tpass;\n" in content)

        // Check static method with Array type conversion
        assert("static func do_thing(items: Array[Node]) -> void:\n\tpass;\n" in content)

        // Check that virtual qualifier is filtered out (only static/vararg kept)
        assert("func _process(delta: float) -> void:\n\tpass;\n" in content)
        // "virtual" should NOT appear
        assert("virtual" !in content)
    }

    @Test
    fun `formats array types correctly`() {
        assertEquals("Array[Node]", ClassParser.formatType("Node[]"))
        assertEquals("String", ClassParser.formatType("String"))
        assertEquals("Array[Array[int]]", ClassParser.formatType("Array[int][]"))
    }

    @Test
    fun `renames var param to variable`(@TempDir tempDir: File) {
        val xmlDir = tempDir.resolve("doc/classes").also { it.mkdirs() }
        xmlDir.resolve("TestClass.xml").writeText("""
            <?xml version="1.0" encoding="UTF-8" ?>
            <class name="TestClass" version="4.6">
                <methods>
                    <method name="set_value">
                        <return type="void" />
                        <param index="0" name="var" type="Variant" />
                    </method>
                </methods>
            </class>
        """.trimIndent())

        val outputDir = tempDir.resolve("output").also { it.mkdirs() }
        ClassParser.parse(listOf(xmlDir), outputDir)

        val content = outputDir.resolve("TestClass.gd").readText()
        assert("variable: Variant" in content)
        assert("var: Variant" !in content.replace("var ", "").replace("\nvar ", ""))
    }

    @Test
    fun `skips members for ProjectSettings`(@TempDir tempDir: File) {
        val xmlDir = tempDir.resolve("doc/classes").also { it.mkdirs() }
        xmlDir.resolve("ProjectSettings.xml").writeText("""
            <?xml version="1.0" encoding="UTF-8" ?>
            <class name="ProjectSettings" inherits="Object" version="4.6">
                <members>
                    <member name="some_setting" type="bool">A setting.</member>
                </members>
            </class>
        """.trimIndent())

        val outputDir = tempDir.resolve("output").also { it.mkdirs() }
        ClassParser.parse(listOf(xmlDir), outputDir)

        val content = outputDir.resolve("ProjectSettings.gd").readText()
        assert("var some_setting" !in content)
    }

    @Test
    fun `replaces at-sign in class name with underscore`(@TempDir tempDir: File) {
        val xmlDir = tempDir.resolve("doc/classes").also { it.mkdirs() }
        xmlDir.resolve("@GlobalScope.xml").writeText("""
            <?xml version="1.0" encoding="UTF-8" ?>
            <class name="@GlobalScope" version="4.6">
            </class>
        """.trimIndent())

        val outputDir = tempDir.resolve("output").also { it.mkdirs() }
        ClassParser.parse(listOf(xmlDir), outputDir)

        val output = outputDir.resolve("_GlobalScope.gd")
        assert(output.exists()) { "File should be named _GlobalScope.gd" }
        assert("class_name _GlobalScope" in output.readText())
    }
}
