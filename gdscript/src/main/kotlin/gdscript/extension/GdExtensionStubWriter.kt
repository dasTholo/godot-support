package gdscript.extension

import com.intellij.openapi.diagnostic.thisLogger
import java.nio.file.Files
import java.nio.file.Path

/**
 * Generates .gd stub files from collected GDExtension type information.
 */
object GdExtensionStubWriter {

    fun writeStubs(types: List<GdExtTypeInfo>, outputDir: Path) {
        if (types.isEmpty()) return

        Files.createDirectories(outputDir)

        // Clean old stubs
        outputDir.toFile().listFiles { f -> f.extension == "gd" }?.forEach { it.delete() }

        for (type in types) {
            val file = outputDir.resolve("${type.name}.gd")
            val content = generateStub(type)
            Files.writeString(file, content)
        }

        thisLogger().info("Wrote ${types.size} GDExtension stubs to $outputDir")
    }

    private fun generateStub(type: GdExtTypeInfo): String {
        val sb = StringBuilder()

        sb.appendLine("extends ${type.inherits}")
        sb.appendLine("class_name ${type.name}")
        sb.appendLine()

        // Signals
        for (signal in type.signals) {
            sb.appendLine("signal ${signal.name}()")
        }
        if (type.signals.isNotEmpty()) sb.appendLine()

        // Properties
        for (prop in type.properties) {
            sb.appendLine("var ${prop.name}: ${prop.type}")
        }
        if (type.properties.isNotEmpty()) sb.appendLine()

        // Methods
        for (method in type.methods) {
            val params = method.params.joinToString(", ") { "${it.first}: ${it.second}" }
            val ret = if (method.returnType == "void") " -> void" else " -> ${method.returnType}"
            val prefix = if (method.name in type.staticMethods) "static func" else "func"
            sb.appendLine("$prefix ${method.name}($params)$ret: pass")
        }

        return sb.toString()
    }
}
