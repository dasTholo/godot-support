package sdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AnnotationParserTest {

    @Test
    fun `parses annotations from GDScript xml`(@TempDir tempDir: File) {
        val inputXml = File(javaClass.classLoader.getResource("annotation_input.xml")!!.toURI())
        val outputFile = tempDir.resolve("annotation.gdconf")

        AnnotationParser.parse(inputXml, outputFile)

        val expected = buildString {
            //        AN _variadic|spaces  required  name(padded to 30)  params
            appendLine("AN          0 export                        ")
            appendLine("AN          1 export_category                name:String")
            appendLine("AN _variadic 1 export_enum                    names:String")
            appendLine("AN          1 export_group                   name:String prefix:String")
            appendLine("AN          0 onready                       ")
        }
        assertEquals(expected, outputFile.readText())
    }
}
