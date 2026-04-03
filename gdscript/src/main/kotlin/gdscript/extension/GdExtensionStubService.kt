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

        // Step 3: Collect details for all extension types via LSP (batched)
        val typeInfos = collector.collectAllTypeDetails(extensionTypes)

        // Step 3b: Enrich with Rust source data (full signatures + base class + static methods + signals)
        val rustResolver = GdExtensionRustResolver.getInstance(project)
        val enrichedTypeInfos = typeInfos.map { type ->
            val rustMethods = rustResolver.collectMethodSignatures(type.name)
            val rustSignals = rustResolver.collectSignals(type.name)
            val baseClass = rustResolver.getBaseClass(type.name)
            val staticMethods = rustResolver.collectStaticMethods(type.name)
            type.copy(
                inherits = baseClass ?: type.inherits,
                staticMethods = staticMethods,
                rustMethods = rustMethods.ifEmpty { null },
                rustSignals = rustSignals.ifEmpty { null }
            )
        }

        // Step 4: Write stub files
        val stubDir = getStubDirectory()
        val sdkPath = try {
            val godotDir = findGodotProjectDir(java.io.File(project.basePath ?: ""))
            val projectFile = godotDir?.resolve("project.godot")
            val content = projectFile?.readText() ?: ""
            val versionMatch = "config/features=PackedStringArray\\(.*\"(\\d\\.\\d)\".*\\)".toRegex().find(content)
            val version = versionMatch?.groupValues?.get(1) ?: "4.4"
            GdLibraryManager.extractSdkIfNeeded(version)
        } catch (_: Exception) { null }

        GdExtensionStubWriter.writeStubs(enrichedTypeInfos, stubDir, sdkPath, project)

        // Step 5: Register as library
        GdLibraryManager.registerLibrary(LIBRARY_NAME, stubDir, project)

        thisLogger().info("GDExtension stub generation complete: ${enrichedTypeInfos.size} types")
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

        // Collect class names from SDK .gd files on disk (not the index, which includes our stubs)
        try {
            val godotDir = findGodotProjectDir(java.io.File(project.basePath ?: ""))
            val projectFile = godotDir?.resolve("project.godot")
            val content = projectFile?.readText() ?: ""
            val versionMatch = "config/features=PackedStringArray\\(.*\"(\\d\\.\\d)\".*\\)".toRegex().find(content)
            val version = versionMatch?.groupValues?.get(1) ?: "4.4"

            val sdkPath = gdscript.library.GdLibraryManager.extractSdkIfNeeded(version)
            val sdkFiles = sdkPath.toFile().listFiles { f -> f.extension == "gd" } ?: emptyArray()
            val sdkTypes = sdkFiles.map { it.nameWithoutExtension }.toSet()
            builtins.addAll(sdkTypes)
            thisLogger().info("Found ${sdkTypes.size} SDK types from $sdkPath")
        } catch (e: Exception) {
            thisLogger().warn("Could not read SDK files: ${e.message}")
        }

        // Collect class_name declarations from project .gd files (to exclude user-defined types)
        try {
            val godotDir = findGodotProjectDir(java.io.File(project.basePath ?: ""))
            if (godotDir != null) {
                val projectTypes = mutableSetOf<String>()
                godotDir.walkTopDown()
                    .filter { it.extension == "gd" && !it.path.contains("_gdext_probe") }
                    .forEach { file ->
                        file.useLines { lines ->
                            for (line in lines) {
                                val match = Regex("^class_name\\s+(\\w+)").find(line.trim())
                                if (match != null) {
                                    projectTypes.add(match.groupValues[1])
                                }
                                // class_name is always near the top, stop early
                                if (line.startsWith("func ") || line.startsWith("var ")) break
                            }
                        }
                    }
                builtins.addAll(projectTypes)
                thisLogger().info("Found ${projectTypes.size} project types to exclude")
            }
        } catch (e: Exception) {
            thisLogger().warn("Could not scan project files: ${e.message}")
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
