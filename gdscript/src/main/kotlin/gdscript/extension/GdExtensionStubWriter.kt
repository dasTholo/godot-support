package gdscript.extension

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import gdscript.index.impl.GdClassNamingIndex
import gdscript.psi.GdClassVarDeclTl
import gdscript.psi.GdMethodDeclTl
import gdscript.psi.GdSignalDeclTl
import gdscript.psi.utils.GdInheritanceUtil
import java.nio.file.Files
import java.nio.file.Path

/**
 * Generates .gd stub files from collected GDExtension type information.
 */
object GdExtensionStubWriter {

    fun writeStubs(types: List<GdExtTypeInfo>, outputDir: Path, sdkPath: Path?, project: Project? = null) {
        if (types.isEmpty()) return

        Files.createDirectories(outputDir)

        // Clean old stubs
        outputDir.toFile().listFiles { f -> f.extension == "gd" }?.forEach { it.delete() }

        // Build inherited member sets from SDK files per base class
        val inheritedCache = mutableMapOf<String, Set<String>>()

        for (type in types) {
            val file = outputDir.resolve("${type.name}.gd")
            val inherited = inheritedCache.getOrPut(type.inherits) {
                if (project != null) {
                    collectInheritedNamesPsi(type.inherits, project)
                } else if (sdkPath != null) {
                    collectInheritedNamesText(type.inherits, sdkPath)
                } else {
                    emptySet()
                }
            }
            val content = generateStub(type, inherited)
            Files.writeString(file, content)
        }

        thisLogger().info("Wrote ${types.size} GDExtension stubs to $outputDir")
    }

    private fun generateStub(type: GdExtTypeInfo, inheritedNames: Set<String>): String {
        val sb = StringBuilder()

        sb.appendLine("extends ${type.inherits}")
        sb.appendLine("class_name ${type.name}")
        sb.appendLine()

        // Signals — prefer Rust-sourced with parameters
        if (type.rustSignals != null) {
            for (signal in type.rustSignals) {
                val params = signal.params.joinToString(", ") { "${it.name}: ${it.type}" }
                sb.appendLine("signal ${signal.name}($params)")
            }
        } else {
            for (signal in type.signals) {
                if (signal.name !in inheritedNames) {
                    sb.appendLine("signal ${signal.name}()")
                }
            }
        }
        if ((type.rustSignals ?: type.signals).isNotEmpty()) sb.appendLine()

        // Properties — skip entirely when Rust data available (inherited), filter for LSP-only
        if (type.rustMethods == null) {
            val filtered = type.properties.filter { it.name !in inheritedNames }
            for (prop in filtered) {
                sb.appendLine("var ${prop.name}: ${prop.type}")
            }
            if (filtered.isNotEmpty()) sb.appendLine()
        }

        // Methods — prefer Rust-sourced signatures when available
        if (type.rustMethods != null) {
            for (method in type.rustMethods) {
                val params = method.params.joinToString(", ") { "${it.name}: ${it.type}" }
                val ret = " -> ${method.returnType}"
                val prefix = if (method.isStatic) "static func" else "func"
                sb.appendLine("$prefix ${method.name}($params)$ret: pass")
            }
        } else {
            for (method in type.methods) {
                if (method.name !in inheritedNames) {
                    val params = method.params.joinToString(", ") { "${it.first}: ${it.second}" }
                    val ret = if (method.returnType == "void") " -> void" else " -> ${method.returnType}"
                    val prefix = if (method.name in type.staticMethods) "static func" else "func"
                    sb.appendLine("$prefix ${method.name}($params)$ret: pass")
                }
            }
        }

        return sb.toString()
    }

    private fun collectInheritedNamesPsi(baseClass: String, project: Project): Set<String> {
        val classElement = GdClassNamingIndex.INSTANCE.getGlobally(baseClass, project).firstOrNull()
            ?.containingFile ?: return emptySet()

        val names = mutableSetOf<String>()
        var current: com.intellij.psi.PsiElement? = classElement
        val visited = mutableSetOf<com.intellij.psi.PsiElement>()
        while (current != null && visited.add(current)) {
            val methods = PsiTreeUtil.getStubChildrenOfTypeAsList(current, GdMethodDeclTl::class.java)
            val vars = PsiTreeUtil.getStubChildrenOfTypeAsList(current, GdClassVarDeclTl::class.java)
            val signals = PsiTreeUtil.getStubChildrenOfTypeAsList(current, GdSignalDeclTl::class.java)
            methods.forEach { names.add(it.name) }
            vars.forEach { names.add(it.name) }
            signals.forEach { names.add(it.name) }
            current = GdInheritanceUtil.getExtendedElement(current, project)
        }
        return names
    }

    /**
     * Collect all member names (methods, properties, signals) from SDK .gd files
     * for a given base class and its ancestors via the `extends` chain.
     */
    private fun collectInheritedNamesText(baseClass: String, sdkPath: Path): Set<String> {
        val names = mutableSetOf<String>()
        var current: String? = baseClass

        while (current != null) {
            val file = sdkPath.resolve("$current.gd").toFile()
            if (!file.exists()) break

            var nextExtends: String? = null
            file.useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("extends ")) {
                        nextExtends = trimmed.removePrefix("extends ").trim()
                    }
                    if (trimmed.startsWith("func ") || trimmed.startsWith("static func ")) {
                        val name = trimmed.removePrefix("static ").removePrefix("func ")
                            .substringBefore("(").trim()
                        names.add(name)
                    }
                    if (trimmed.startsWith("var ")) {
                        val name = trimmed.removePrefix("var ").substringBefore(":").substringBefore(" ").trim()
                        names.add(name)
                    }
                    if (trimmed.startsWith("signal ")) {
                        val name = trimmed.removePrefix("signal ").substringBefore("(").trim()
                        names.add(name)
                    }
                }
            }
            current = nextExtends
        }

        return names
    }
}
