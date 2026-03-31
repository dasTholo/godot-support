package sdk

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object AnnotationParser {

    fun parse(xmlFile: File, outputFile: File) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val annotations = doc.getElementsByTagName("annotation")
        val sb = StringBuilder()

        for (i in 0 until annotations.length) {
            val ann = annotations.item(i) as Element
            val name = ann.getAttribute("name").removePrefix("@")
            val qualifiers = ann.getAttribute("qualifiers") ?: ""
            val isVariadic = qualifiers == "vararg"
            val varargStr = if (isVariadic) "_variadic" else " ".repeat("_variadic".length - 1)

            val params = ann.getElementsByTagName("param")
            var required = 0
            val paramParts = mutableListOf<String>()

            for (j in 0 until params.length) {
                val param = params.item(j) as Element
                val pName = param.getAttribute("name")
                val pType = param.getAttribute("type").ifEmpty { "Variant" }
                paramParts.add("$pName:$pType")
                if (!param.hasAttribute("default")) {
                    required++
                }
            }

            val targetWidth = if (paramParts.isEmpty()) 30 else 31
            val paramSpace = " ".repeat(maxOf(1, targetWidth - name.length))
            val paramLine = paramParts.joinToString(" ")

            sb.appendLine("AN $varargStr $required $name$paramSpace$paramLine")
        }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(sb.toString())
    }
}
