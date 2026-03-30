package gdscript.extension

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import gdscript.library.GdLibraryManager
import godot.lsp.settings.GodotLspSettings
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class GdExtensionStubService(private val project: Project) {

    companion object {
        private const val LIBRARY_NAME = "GdExtensionStubs"

        fun getInstance(project: Project): GdExtensionStubService =
            project.getService(GdExtensionStubService::class.java)
    }

    fun generateStubs() {
        val port = GodotLspSettings.getInstance().lspPort
        val basePath = project.basePath ?: return
        val godotDir = findGodotProjectDir(java.io.File(basePath))
        val projectUri = "file://${godotDir?.absolutePath ?: basePath}"

        thisLogger().info("Starting GDExtension stub generation via Godot LSP on port $port")

        GdExtensionLspClient(port).use { client ->
            if (!client.connect(projectUri)) {
                thisLogger().info("Godot LSP not available on port $port - skipping stub generation")
                return
            }

            val collector = GdExtensionTypeCollector(client)

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
                    val info = collector.collectTypeDetails(typeName)
                    // Only include types that actually have GDExtension-specific methods
                    // (not just inherited ones) or are instantiable custom types
                    info
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
    }

    private fun getStubDirectory(): Path {
        val projectHash = project.basePath.hashCode().toUInt().toString(16)
        return PathManager.getConfigDir().resolve("gdext-stubs").resolve(projectHash)
    }

    private fun getKnownSdkTypes(): Set<String> {
        // Built-in primitive types
        val builtins = setOf("int", "String", "float", "bool", "Array", "Dictionary",
            "void", "Variant", "Callable", "StringName", "NodePath")

        // SDK types are resolved via the existing library - collect their class_name declarations
        // For now, we rely on the fact that SDK stubs exist as .gd files in the registered library.
        // Types that exist in the SDK will have resolveRef() succeed anyway, so even if we
        // generate a duplicate stub, the worst case is a harmless extra file.
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
