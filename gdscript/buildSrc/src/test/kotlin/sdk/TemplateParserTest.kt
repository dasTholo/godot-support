package sdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TemplateParserTest {

    @Test
    fun `converts templates replacing BASE with NAME`(@TempDir tempDir: File) {
        // Set up input structure: script_templates/CharacterBody2D/basic_movement.gd
        val templateDir = tempDir.resolve("script_templates/CharacterBody2D").also { it.mkdirs() }
        templateDir.resolve("basic_movement.gd").writeText(
            """
            extends _BASE_

            func _ready():
                pass
            """.trimIndent()
        )

        val outputDir = tempDir.resolve("output").also { it.mkdirs() }

        TemplateParser.parse(tempDir.resolve("script_templates"), outputDir)

        val outputFile = outputDir.resolve("CharacterBody2D basic_movement.gd.ft")
        assertTrue(outputFile.exists(), "Output file should exist")
        assertEquals(
            """
            extends ${'$'}{NAME}

            func _ready():
                pass
            """.trimIndent(),
            outputFile.readText()
        )
    }

    @Test
    fun `skips hidden directories`(@TempDir tempDir: File) {
        val templateDir = tempDir.resolve("script_templates/.hidden").also { it.mkdirs() }
        templateDir.resolve("test.gd").writeText("extends _BASE_")

        val outputDir = tempDir.resolve("output").also { it.mkdirs() }

        TemplateParser.parse(tempDir.resolve("script_templates"), outputDir)

        assertEquals(0, outputDir.listFiles()?.size ?: 0)
    }
}
