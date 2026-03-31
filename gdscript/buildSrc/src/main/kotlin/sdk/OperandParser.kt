package sdk

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object OperandParser {

    fun parse(xmlDirs: List<File>, outputFile: File) {
        val dbf = DocumentBuilderFactory.newInstance()
        // class name -> operator -> (rightType -> resultType)
        val operators = linkedMapOf<String, LinkedHashMap<String, LinkedHashMap<String, String>>>()
        val operatorPrefix = "operator "

        for (xmlDir in xmlDirs) {
            if (!xmlDir.isDirectory) continue
            for (file in xmlDir.listFiles().orEmpty().sorted()) {
                if (!file.name.endsWith(".xml")) continue
                val className = file.nameWithoutExtension
                if (className.startsWith("@")) continue

                val doc = dbf.newDocumentBuilder().parse(file)
                val ops = doc.getElementsByTagName("operator")

                for (i in 0 until ops.length) {
                    val op = ops.item(i) as Element
                    val rawName = op.getAttribute("name")
                    val opSymbol = rawName.removePrefix(operatorPrefix)

                    if ("unary" in opSymbol) continue
                    if (opSymbol == "~") continue

                    val returnEl = op.getElementsByTagName("return").item(0) as Element
                    val resultType = returnEl.getAttribute("type")

                    val paramEl = op.getElementsByTagName("param").item(0) as Element
                    val rightType = paramEl.getAttribute("type")

                    operators
                        .getOrPut(className) { linkedMapOf() }
                        .getOrPut(opSymbol) { linkedMapOf() }[rightType] = resultType
                }
            }
        }

        val sb = StringBuilder()
        for ((className, opMap) in operators) {
            sb.appendLine("OP $className")
            for ((opSymbol, operands) in opMap) {
                for ((rightType, resultType) in operands) {
                    sb.appendLine("$opSymbol $rightType : $resultType")
                }
            }
            sb.appendLine()
        }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(sb.toString())
    }
}
