package gdscript.extension

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import gdscript.library.GdLibraryManager
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class GdExtensionStubService(private val project: Project) {

    companion object {
        private const val LIBRARY_NAME = "GdExtensionStubs"

        fun getInstance(project: Project): GdExtensionStubService =
            project.getService(GdExtensionStubService::class.java)
    }

    fun generateStubs() {
        val basePath = project.basePath ?: return
        val godotDir = findGodotProjectDir(java.io.File(basePath))
        val godotPath = godotDir?.absolutePath ?: basePath

        thisLogger().info("Starting GDExtension stub generation via existing Godot LSP connection")

        val collector = GdExtensionTypeCollector(project, godotPath)

        // Step 1: Get all type names from LSP
        val allTypeNames = collector.collectAllTypeNames()
        if (allTypeNames.isEmpty()) {
            thisLogger().warn("No types returned from Godot LSP")
            return
        }

        // Step 2: Filter to GDExtension-only types
        val knownTypes = getKnownSdkTypes()
        val extensionTypes = allTypeNames.filter { it !in knownTypes }

        if (extensionTypes.isEmpty()) {
            thisLogger().info("No GDExtension types found")
            return
        }

        thisLogger().info("Found ${extensionTypes.size} potential GDExtension types, collecting details...")

        // Step 3: Collect details for each extension type
        val typeInfos = extensionTypes.mapNotNull { typeName ->
            try {
                collector.collectTypeDetails(typeName)
            } catch (e: Exception) {
                thisLogger().warn("Failed to collect details for $typeName: ${e.message}")
                null
            }
        }

        // Step 4: Write stub files
        val stubDir = getStubDirectory()
        GdExtensionStubWriter.writeStubs(typeInfos, stubDir)

        // Step 5: Register as library
        GdLibraryManager.registerLibrary(LIBRARY_NAME, stubDir, project)

        thisLogger().info("GDExtension stub generation complete: ${typeInfos.size} types")
    }

    private fun getStubDirectory(): Path {
        val projectHash = project.basePath.hashCode().toUInt().toString(16)
        return PathManager.getConfigDir().resolve("gdext-stubs").resolve(projectHash)
    }

    private fun getKnownSdkTypes(): Set<String> {
        val builtins = setOf("int", "String", "float", "bool", "Array", "Dictionary",
            "void", "Variant", "Callable", "StringName", "NodePath")
        return builtins
    }

    private fun findGodotProjectDir(dir: java.io.File, maxDepth: Int = 3): java.io.File? {
        if (java.io.File(dir, "project.godot").exists()) return dir
        if (maxDepth <= 0) return null
        dir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach { sub ->
            findGodotProjectDir(sub, maxDepth - 1)?.let { return it }
        }
        return null
    }
}
