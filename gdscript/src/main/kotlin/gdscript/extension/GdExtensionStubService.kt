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

        thisLogger().info("Found ${extensionTypes.size} potential GDExtension types, generating stubs...")

        // Step 3: Create basic stubs for each extension type
        // We generate minimal stubs with class_name so the indexer can resolve them.
        // Full method details would require per-type LSP queries which is too slow.
        val typeInfos = extensionTypes.map { typeName ->
            GdExtTypeInfo(typeName, "RefCounted", emptyList(), emptyList(), emptyList())
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
        val builtins = mutableSetOf("int", "String", "float", "bool", "Array", "Dictionary",
            "void", "Variant", "Callable", "StringName", "NodePath", "Signal",
            "Vector2", "Vector2i", "Vector3", "Vector3i", "Vector4", "Vector4i",
            "Color", "Rect2", "Rect2i", "Transform2D", "Transform3D",
            "Basis", "Quaternion", "AABB", "Plane", "Projection",
            "RID", "PackedByteArray", "PackedInt32Array", "PackedInt64Array",
            "PackedFloat32Array", "PackedFloat64Array", "PackedStringArray",
            "PackedVector2Array", "PackedVector3Array", "PackedColorArray",
            "PackedVector4Array")

        // Collect class_name from SDK stub files via the GdClassNamingIndex
        try {
            val sdkTypes = gdscript.index.impl.GdClassNamingIndex.INSTANCE.getAllKeys(project)
            builtins.addAll(sdkTypes)
            thisLogger().info("Found ${sdkTypes.size} types from SDK index")
        } catch (e: Exception) {
            thisLogger().warn("Could not read SDK index: ${e.message}")
        }

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
