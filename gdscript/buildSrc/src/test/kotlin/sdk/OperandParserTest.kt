package sdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OperandParserTest {

    @Test
    fun `parses operators skipping unary and tilde`(@TempDir tempDir: File) {
        val xmlDir = tempDir.resolve("doc/classes").also { it.mkdirs() }
        val inputXml = File(javaClass.classLoader.getResource("operand_input.xml")!!.toURI())
        inputXml.copyTo(xmlDir.resolve("Vector2.xml"))

        val outputFile = tempDir.resolve("operators.gdconf")

        OperandParser.parse(listOf(xmlDir), outputFile)

        val expected = buildString {
            appendLine("OP Vector2")
            appendLine("!= Vector2 : bool")
            appendLine("* float : Vector2")
            appendLine("== Vector2 : bool")
            appendLine()
        }
        assertEquals(expected, outputFile.readText())
    }

    @Test
    fun `skips files starting with at-sign`(@TempDir tempDir: File) {
        val xmlDir = tempDir.resolve("doc/classes").also { it.mkdirs() }
        val inputXml = File(javaClass.classLoader.getResource("operand_input.xml")!!.toURI())
        inputXml.copyTo(xmlDir.resolve("@GDScript.xml"))

        val outputFile = tempDir.resolve("operators.gdconf")

        OperandParser.parse(listOf(xmlDir), outputFile)

        assertEquals("", outputFile.readText())
    }
}
